package com.trivoko.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.dto.BrandCount;
import com.trivoko.catalog.dto.CartVariant;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductDetail;
import com.trivoko.catalog.dto.SellerProductRequest;
import com.trivoko.catalog.dto.SellerProductView;
import com.trivoko.catalog.dto.SellerVariantRequest;
import com.trivoko.catalog.dto.VariantView;
import com.trivoko.admin.AuditService;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.common.Slugs;
import com.trivoko.seller.Seller;
import com.trivoko.seller.SellerStatus;

import jakarta.persistence.EntityManager;

/** The catalogue's rules. Other modules call THIS class, never ProductRepository (ArchUnit rule). */
@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository products;
	private final ProductVariantRepository variants;
	private final PriceHistoryRepository priceHistory;
	private final CategoryService categoryService;
	private final CategoryRepository categories;
	private final AuditService audit;
	private final EntityManager em;

	ProductService(ProductRepository products, ProductVariantRepository variants,
			PriceHistoryRepository priceHistory, CategoryService categoryService, CategoryRepository categories,
			AuditService audit, EntityManager em) {
		this.products = products;
		this.variants = variants;
		this.priceHistory = priceHistory;
		this.categoryService = categoryService;
		this.categories = categories;
		this.audit = audit;
		this.em = em;
	}

	/** The shop listing: only what the public may see, with the customer's filters. */
	public PageResponse<ProductCard> search(ProductSearch search, Pageable pageable) {
		Specification<Product> spec = ProductSpecifications.visibleInShop();
		if (search.category() != null && !search.category().isBlank()) {
			spec = spec.and(ProductSpecifications.inCategories(categoryService.idsWithChildren(search.category())));
		}
		if (search.brands() != null && !search.brands().isEmpty()) {
			spec = spec.and(ProductSpecifications.brandIn(search.brands()));
		}
		if (search.minPrice() != null) {
			spec = spec.and(ProductSpecifications.priceAtLeast(search.minPrice()));
		}
		if (search.maxPrice() != null) {
			spec = spec.and(ProductSpecifications.priceAtMost(search.maxPrice()));
		}
		if (search.inStockOnly()) {
			spec = spec.and(ProductSpecifications.inStock());
		}
		if (search.seller() != null && !search.seller().isBlank()) {
			spec = spec.and(ProductSpecifications.soldBy(search.seller()));
		}
		if (search.q() != null && !search.q().isBlank()) {
			spec = spec.and(ProductSpecifications.matchesWords(search.q()));
		}
		return PageResponse.from(products.findAll(spec, pageable), CatalogMapper::toCard);
	}

	/**
	 * Today's facts about the variants in a cart (Phase 3, cart module). Unknown ids are simply missing from
	 * the map. available = the product is ACTIVE and its shop APPROVED (a blocked shop's items stay in carts
	 * but cannot be bought).
	 */
	public Map<Long, CartVariant> cartVariants(Collection<Long> variantIds) {
		if (variantIds.isEmpty()) {
			return Map.of();
		}
		return variants.findByIdIn(variantIds).stream().collect(Collectors.toMap(ProductVariant::getId, v -> {
			Product p = v.getProduct();
			Seller s = p.getSeller();
			return new CartVariant(v.getId(), p.getSlug(), p.getName(), v.getLabel(),
					p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl(), v.getPrice(), v.getMrp(),
					v.getStock(), p.getStatus() == ProductStatus.ACTIVE && s.getStatus() == SellerStatus.APPROVED,
					s.getId(), s.getShopName(), s.getSlug(), s.getCity());
		}));
	}

	/**
	 * Checkout (Phase 4): hold the stock of every line, or none. Lines are taken in variant-id order, so two
	 * checkouts with the same items always lock rows in the same order (no deadlock). The first line that cannot
	 * be taken throws, and the caller's transaction rolls back every hold made before it.
	 */
	@Transactional
	public void holdStock(Map<Long, Integer> quantities) {
		for (Long variantId : new java.util.TreeSet<>(quantities.keySet())) {
			int qty = quantities.get(variantId);
			if (variants.take(variantId, qty) == 0) {
				CartVariant v = cartVariants(List.of(variantId)).get(variantId);
				Integer left = variants.currentStock(variantId);
				String what = v == null ? "An item" : v.productName() + " (" + v.variantLabel() + ")";
				throw new BusinessRuleException(left == null || left <= 0
						? "Sorry, " + what + " is sold out."
						: "Sorry, " + what + " has only " + left + " left.");
			}
		}
	}

	/**
	 * Like holdStock, but answers true/false instead of throwing, and gives back what it already took when a
	 * later line fails. For a LATE payment: "is all the stock still there? then take it; if not, refund" - an
	 * exception here would mark the caller's transaction rollback-only and the refund could not be saved.
	 */
	@Transactional
	public boolean tryHoldStock(Map<Long, Integer> quantities) {
		Map<Long, Integer> taken = new java.util.LinkedHashMap<>();
		for (Long variantId : new java.util.TreeSet<>(quantities.keySet())) {
			int qty = quantities.get(variantId);
			if (variants.take(variantId, qty) == 0) {
				taken.forEach(variants::giveBack);
				return false;
			}
			taken.put(variantId, qty);
		}
		return true;
	}

	/** Puts held stock back (an unpaid order expired, or a payment was refunded). */
	@Transactional
	public void releaseStock(Map<Long, Integer> quantities) {
		quantities.forEach(variants::giveBack);
	}

	/**
	 * The brand checkboxes of the filter: brands of the visible products, with how many each has.
	 * No category = every brand in the shop. An unknown category simply has no brands (empty list).
	 */
	public List<BrandCount> brands(String category) {
		if (category == null || category.isBlank()) {
			return products.countVisibleByBrand();
		}
		List<Long> categoryIds = categoryService.idsWithChildrenOrEmpty(category);
		return categoryIds.isEmpty() ? List.of() : products.countVisibleByBrandIn(categoryIds);
	}

	/** The product page. A product that is not live (draft, pending, blocked ...) is "not found" for the public. */
	public ProductDetail getBySlug(String slug) {
		return products.findOne(ProductSpecifications.visibleInShop()
				.and((root, query, cb) -> cb.equal(root.get("slug"), slug)))
			.map(CatalogMapper::toDetail)
			.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
	}

	/**
	 * The ONLY way a variant's price changes (seller dashboard in Phase 5, admin, flash sale ...).
	 * Because everything goes through here, price_history never misses a change and the
	 * product's "from" price always matches its cheapest variant.
	 */
	@Transactional
	public VariantView changePrice(Long variantId, BigDecimal newPrice, BigDecimal newMrp) {
		if (newPrice == null || newPrice.signum() <= 0) {
			throw new BadRequestException("price must be greater than 0");
		}
		if (newMrp == null || newMrp.compareTo(newPrice) < 0) {
			throw new BadRequestException("MRP must not be lower than the price");
		}
		ProductVariant variant = variants.findById(variantId)
			.orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));

		BigDecimal oldPrice = variant.getPrice();
		if (oldPrice.compareTo(newPrice) != 0) {
			priceHistory.save(new PriceHistory(variant, oldPrice, newPrice));
			audit.record("PRICE_CHANGED", "VARIANT", variant.getId(), oldPrice.toPlainString() + " -> " + newPrice.toPlainString());
		}
		variant.setPrice(newPrice);
		variant.setMrp(newMrp);
		variant.getProduct().refreshPriceFrom();
		return CatalogMapper.toView(variant);
	}


	// ---------- The seller's own products (Phase 2; the seller module checks ownership first) ----------

	/** A new product in DRAFT: only its seller can see it until the admin approves it. */
	@Transactional
	public SellerProductView createDraft(Long sellerId, SellerProductRequest request) {
		Category category = subCategory(request.categoryId());
		checkPrices(request);
		Product product = new Product(em.getReference(Seller.class, sellerId), category, request.name().trim(),
				Slugs.unique(request.name(), "product", slug -> products.findBySlug(slug).isPresent()),
				request.brand().trim(), request.description().trim());
		products.save(product); // IDENTITY: the id exists now, the SKUs below use it
		request.variants().forEach(v -> product.addVariant(newVariant(product, v)));
		products.flush(); // variant ids for the answer
		return toSellerView(product);
	}

	/** My products, newest first, optionally only one status (e.g. DRAFT). */
	public PageResponse<SellerProductView> findForSeller(Long sellerId, ProductStatus status, Pageable pageable) {
		Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("seller").get("id"), sellerId);
		if (status != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}
		return PageResponse.from(products.findAll(spec, pageable), ProductService::toSellerView);
	}

	public SellerProductView getForSeller(Long productId) {
		return toSellerView(get(productId));
	}

	/** Whose product is this? Empty = no such product. Used by SellerAccess.ownsProduct. */
	public Optional<Long> sellerIdOf(Long productId) {
		return products.findById(productId).map(p -> p.getSeller().getId());
	}

	/**
	 * DRAFT or REJECTED: everything may change (variants are matched by id; missing ones are removed).
	 * ACTIVE: only price, MRP and stock (spec P2-11) - a live product cannot be secretly renamed.
	 * PENDING (waiting for the admin) or BLOCKED: no changes.
	 */
	@Transactional
	public SellerProductView updateBySeller(Long productId, SellerProductRequest request) {
		Product product = get(productId);
		checkPrices(request);
		switch (product.getStatus()) {
			case DRAFT, REJECTED -> fullEdit(product, request);
			case ACTIVE -> liveEdit(product, request);
			case PENDING -> throw new BusinessRuleException("This product is waiting for approval and cannot be changed now.");
			case BLOCKED -> throw new BusinessRuleException("This product is blocked by TriVoKo and cannot be changed.");
		}
		products.flush(); // ids of new variants for the answer
		return toSellerView(product);
	}

	/** DRAFT or REJECTED -> PENDING (the admin's queue). */
	@Transactional
	public SellerProductView submit(Long productId) {
		Product product = get(productId);
		if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
			throw new BusinessRuleException("Only a DRAFT or REJECTED product can be sent for approval (this one is "
					+ product.getStatus() + ").");
		}
		product.setStatus(ProductStatus.PENDING);
		product.setRejectionReason(null);
		return toSellerView(product);
	}

	// ---------- The admin's decisions (Phase 2 endpoints; the admin pages come in Phase 6) ----------

	/** The admin's queue, oldest first (first come, first served). */
	public List<SellerProductView> listByStatus(ProductStatus status) {
		return products.findAll((root, query, cb) -> cb.equal(root.get("status"), status), Sort.by("id")).stream()
			.map(ProductService::toSellerView)
			.toList();
	}

	/** PENDING -> ACTIVE: customers can see and buy it now. The shop itself must still be APPROVED. */
	@Transactional
	public SellerProductView approve(Long productId) {
		Product product = pending(productId);
		if (product.getSeller().getStatus() != SellerStatus.APPROVED) {
			throw new BusinessRuleException("The shop of this product is " + product.getSeller().getStatus()
					+ ", so the product cannot go live.");
		}
		product.setStatus(ProductStatus.ACTIVE);
		product.setRejectionReason(null);
		audit.record("PRODUCT_APPROVED", "PRODUCT", product.getId(), product.getName());
		return toSellerView(product);
	}

	/** PENDING -> REJECTED with a reason the seller sees; they can fix it and submit again. */
	@Transactional
	public SellerProductView reject(Long productId, String reason) {
		Product product = pending(productId);
		product.setStatus(ProductStatus.REJECTED);
		product.setRejectionReason(reason.trim());
		audit.record("PRODUCT_REJECTED", "PRODUCT", product.getId(), reason.trim());
		return toSellerView(product);
	}

	private Product pending(Long productId) {
		Product product = get(productId);
		if (product.getStatus() != ProductStatus.PENDING) {
			throw new BusinessRuleException("Only a PENDING product can be approved or rejected (this one is "
					+ product.getStatus() + ").");
		}
		return product;
	}

	private void fullEdit(Product product, SellerProductRequest request) {
		if (!product.getName().equals(request.name().trim())) {
			product.setSlug(Slugs.unique(request.name(), "product",
					slug -> products.findBySlug(slug).filter(p -> !p.getId().equals(product.getId())).isPresent()));
		}
		product.setName(request.name().trim());
		product.setCategory(subCategory(request.categoryId()));
		product.setBrand(request.brand().trim());
		product.setDescription(request.description().trim());

		Map<Long, ProductVariant> existing = product.getVariants().stream()
			.collect(Collectors.toMap(ProductVariant::getId, v -> v));
		Set<Long> kept = new HashSet<>();
		for (SellerVariantRequest v : request.variants()) {
			if (v.id() == null) {
				product.addVariant(newVariant(product, v));
				continue;
			}
			ProductVariant variant = existing.get(v.id());
			if (variant == null) {
				throw new BadRequestException("Variant " + v.id() + " does not belong to this product");
			}
			kept.add(v.id());
			variant.setLabel(v.label().trim());
			variant.setSize(blankToNull(v.size()));
			variant.setColour(blankToNull(v.colour()));
			variant.setStock(v.stock());
			changePrice(variant.getId(), v.price(), v.mrp());
		}
		// variants the seller left out of the form are removed (orphanRemoval deletes the rows)
		product.getVariants().removeIf(variant -> existing.containsKey(variant.getId()) && !kept.contains(variant.getId()));
		product.refreshPriceFrom();
	}

	private void liveEdit(Product product, SellerProductRequest request) {
		boolean sameDetails = product.getName().equals(request.name().trim())
				&& product.getCategory().getId().equals(request.categoryId())
				&& product.getBrand().equals(request.brand().trim())
				&& Objects.equals(product.getDescription(), request.description().trim());
		Map<Long, ProductVariant> existing = product.getVariants().stream()
			.collect(Collectors.toMap(ProductVariant::getId, v -> v));
		boolean sameVariants = request.variants().size() == existing.size()
				&& request.variants().stream().allMatch(v -> v.id() != null && existing.containsKey(v.id())
						&& existing.get(v.id()).getLabel().equals(v.label().trim())
						&& Objects.equals(existing.get(v.id()).getSize(), blankToNull(v.size()))
						&& Objects.equals(existing.get(v.id()).getColour(), blankToNull(v.colour())));
		if (!sameDetails || !sameVariants) {
			throw new BusinessRuleException("A live product can only change its prices and stock. "
					+ "To change anything else, contact TriVoKo support.");
		}
		for (SellerVariantRequest v : request.variants()) {
			existing.get(v.id()).setStock(v.stock());
			changePrice(v.id(), v.price(), v.mrp());
		}
	}

	private Product get(Long productId) {
		return products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
	}

	/** Products sit on the lower shelf (e.g. "Mobile phones"), never on a top category ("Electronics"). */
	private Category subCategory(Long categoryId) {
		Category category = categories.findById(categoryId)
			.orElseThrow(() -> new BadRequestException("Category " + categoryId + " does not exist"));
		if (category.getParent() == null) {
			throw new BadRequestException("Choose a sub-category (e.g. Mobile phones), not a top category");
		}
		return category;
	}

	private static void checkPrices(SellerProductRequest request) {
		for (SellerVariantRequest v : request.variants()) {
			if (v.mrp().compareTo(v.price()) < 0) {
				throw new BadRequestException("MRP must not be lower than the price (variant '" + v.label() + "')");
			}
		}
	}

	/** SKU = "P" + product id + a short random part, e.g. P121-7F3A2C (unique, never reused). */
	private static ProductVariant newVariant(Product product, SellerVariantRequest v) {
		String sku = "P" + product.getId() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
		return new ProductVariant(sku, v.label().trim(), blankToNull(v.size()), blankToNull(v.colour()), v.price(),
				v.mrp(), v.stock());
	}

	private static SellerProductView toSellerView(Product p) {
		List<SellerProductView.Variant> vs = p.getVariants().stream()
			.sorted(Comparator.comparing(ProductVariant::getId, Comparator.nullsLast(Comparator.naturalOrder())))
			.map(v -> new SellerProductView.Variant(v.getId(), v.getSku(), v.getLabel(), v.getSize(), v.getColour(),
					v.getPrice(), v.getMrp(), v.getStock()))
			.toList();
		return new SellerProductView(p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getDescription(),
				p.getCategory().getId(), p.getCategory().getName(), p.getStatus(), p.getRejectionReason(),
				p.getPriceFrom(), p.getMrpFrom(), vs, p.getCreatedAt(), p.getUpdatedAt());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}

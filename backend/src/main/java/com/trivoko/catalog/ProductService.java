package com.trivoko.catalog;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductDetail;
import com.trivoko.catalog.dto.VariantView;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;

/** The catalogue's rules. Other modules call THIS class, never ProductRepository (ArchUnit rule). */
@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository products;
	private final ProductVariantRepository variants;
	private final PriceHistoryRepository priceHistory;
	private final CategoryService categoryService;

	ProductService(ProductRepository products, ProductVariantRepository variants,
			PriceHistoryRepository priceHistory, CategoryService categoryService) {
		this.products = products;
		this.variants = variants;
		this.priceHistory = priceHistory;
		this.categoryService = categoryService;
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
		return PageResponse.from(products.findAll(spec, pageable), CatalogMapper::toCard);
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
		}
		variant.setPrice(newPrice);
		variant.setMrp(newMrp);
		variant.getProduct().refreshPriceFrom();
		return CatalogMapper.toView(variant);
	}

}

package com.trivoko.catalog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductDetail;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;

/** The catalogue's rules. Other modules call THIS class, never ProductRepository (ArchUnit rule). */
@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository products;
	private final CategoryService categoryService;

	ProductService(ProductRepository products, CategoryService categoryService) {
		this.products = products;
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

}

package com.trivoko.catalog;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.trivoko.seller.SellerStatus;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

/**
 * Small WHERE pieces for the product listing. ProductService joins the ones the customer chose
 * with AND, and Spring Data turns them into ONE SQL query:
 *
 * <pre>
 *   ?category=phones&maxPrice=15000&inStock=true
 *   -> WHERE status='ACTIVE' AND seller.status='APPROVED'
 *        AND category_id IN (2) AND price_from <= 15000
 *        AND EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.id AND v.stock > 0)
 * </pre>
 */
final class ProductSpecifications {

	/** What the public may see: ACTIVE products of APPROVED shops. */
	static Specification<Product> visibleInShop() {
		return (root, query, cb) -> cb.and(
				cb.equal(root.get("status"), ProductStatus.ACTIVE),
				cb.equal(root.get("seller").get("status"), SellerStatus.APPROVED));
	}

	static Specification<Product> inCategories(Collection<Long> categoryIds) {
		return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
	}

	static Specification<Product> brandIn(Collection<String> brands) {
		return (root, query, cb) -> root.get("brand").in(brands);
	}

	static Specification<Product> priceAtLeast(BigDecimal min) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("priceFrom"), min);
	}

	static Specification<Product> priceAtMost(BigDecimal max) {
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("priceFrom"), max);
	}

	/**
	 * The simple search box (Phase 3; smart search replaces it in Phase 10): EVERY word must appear in the
	 * name or the brand, e.g. "volta cover" -> name/brand LIKE '%volta%' AND name/brand LIKE '%cover%'.
	 * % and _ typed by the customer are escaped, so "100%" means the text "100%", not "100 + anything".
	 */
	static Specification<Product> matchesWords(String q) {
		return (root, query, cb) -> cb.and(Arrays.stream(q.trim().toLowerCase(Locale.ROOT).split("\\s+"))
			.map(word -> "%" + word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%")
			.map(pattern -> cb.or(
					cb.like(cb.lower(root.get("name")), pattern, '\\'),
					cb.like(cb.lower(root.get("brand")), pattern, '\\')))
			.toArray(Predicate[]::new));
	}

	static Specification<Product> soldBy(String sellerSlug) {
		return (root, query, cb) -> cb.equal(root.get("seller").get("slug"), sellerSlug);
	}

	/** At least one variant has stock. */
	static Specification<Product> inStock() {
		return (root, query, cb) -> {
			Subquery<Long> variantInStock = query.subquery(Long.class);
			var variant = variantInStock.from(ProductVariant.class);
			variantInStock.select(variant.get("id")).where(
					cb.equal(variant.get("product"), root),
					cb.greaterThan(variant.get("stock"), 0));
			return cb.exists(variantInStock);
		};
	}

	private ProductSpecifications() {
	}

}

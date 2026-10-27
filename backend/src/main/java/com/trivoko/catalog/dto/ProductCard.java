package com.trivoko.catalog.dto;

import java.math.BigDecimal;

/** One tile in the product grid: photo, name, "from" price, % off, shop, stars. */
public record ProductCard(
		Long id,
		String slug,
		String name,
		String brand,
		BigDecimal priceFrom,
		BigDecimal mrpFrom,
		int discountPercent,
		String imageUrl,
		boolean inStock,
		String sellerName,
		String sellerSlug,
		String categorySlug,
		/** Average stars (null = no reviews yet) and how many reviews (Phase 9). */
		BigDecimal ratingAvg,
		int ratingCount) {
}

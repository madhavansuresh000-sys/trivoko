package com.trivoko.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

/** Everything the product page needs in one call. */
public record ProductDetail(
		Long id,
		String slug,
		String name,
		String brand,
		String description,
		BigDecimal priceFrom,
		BigDecimal mrpFrom,
		int discountPercent,
		CategoryRef category,
		SellerRef seller,
		List<VariantView> variants,
		List<ImageView> images,
		BigDecimal ratingAvg,
		int ratingCount,
		/** The lowest "from" price in the last 30 days (today's price included). */
		BigDecimal lowestPrice30Days,
		/** true = the price moved in those 30 days and today's price IS the lowest -> green badge. */
		boolean lowestNow) {

	/** A category with its parent, for the breadcrumb: Mobiles & Accessories > Cases & covers. */
	public record CategoryRef(String name, String slug, CategoryRef parent) {
	}

	/** "Sold by Chennai Mobiles, Chennai". */
	public record SellerRef(String shopName, String slug, String city) {
	}

	public record ImageView(String url, String altText) {
	}

}

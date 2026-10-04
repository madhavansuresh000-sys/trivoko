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
		List<ImageView> images) {

	/** A category with its parent, for the breadcrumb: Mobiles & Accessories > Cases & covers. */
	public record CategoryRef(String name, String slug, CategoryRef parent) {
	}

	/** "Sold by Chennai Mobiles, Chennai". */
	public record SellerRef(String shopName, String slug, String city) {
	}

	public record ImageView(String url, String altText) {
	}

}

package com.trivoko.catalog.dto;

import java.math.BigDecimal;

/**
 * What the cart needs to know about one variant RIGHT NOW (ProductService.cartVariants).
 * available = the product is ACTIVE and its shop is APPROVED; the exact stock stays on the server
 * (the cart view only shows "only N left").
 */
public record CartVariant(
		Long variantId,
		String productSlug,
		String productName,
		String variantLabel,
		String imageUrl,
		BigDecimal price,
		BigDecimal mrp,
		int stock,
		boolean available,
		Long sellerId,
		String sellerName,
		String sellerSlug,
		String sellerCity) {
}

package com.trivoko.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The cart as the cart page shows it: one package per seller (the order split is visible BEFORE paying).
 * itemCount = quantities of the lines that can be bought (the navbar badge). Delivery is shown now and
 * charged in Phase 4 checkout, where the server calculates every amount again.
 */
public record CartView(
		List<CartPackage> packages,
		int itemCount,
		BigDecimal itemsTotal,
		BigDecimal shippingTotal,
		BigDecimal total) {

	/** One seller's box: its lines, their total and its own delivery fee. */
	public record CartPackage(SellerRef seller, List<CartLineView> items, BigDecimal itemsTotal, BigDecimal shippingFee) {
	}

	public record SellerRef(String name, String slug, String city) {
	}

	/**
	 * One line. maxQuantity = the most this line may have now (10, or less when the stock is low; 0 when it
	 * cannot be bought). note = why a line looks different, e.g. "Only 2 left - quantity lowered".
	 */
	public record CartLineView(
			Long variantId,
			String productSlug,
			String productName,
			String variantLabel,
			String imageUrl,
			BigDecimal price,
			BigDecimal mrp,
			int quantity,
			int maxQuantity,
			boolean available,
			String note) {
	}

}

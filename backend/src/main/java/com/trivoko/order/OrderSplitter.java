package com.trivoko.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * ★ THE ORDER SPLIT - written by Madhavan (Phase 4, step 4). Pure Java: no database, no Spring.
 *
 * <pre>
 *   cart lines (seller, price, qty)  +  coupon discount (from CouponService)
 *        |
 *        v  split(...)
 *   one PackagePlan per seller (in the order the sellers first appear)
 *     - delivery per package: items total (BEFORE discount) >= Rs 499 -> 0, else Rs 40
 *     - each item's discount share = discount x lineTotal / order itemsTotal, rounded to paise (HALF_UP);
 *       the LAST item of the whole order gets the remainder, so the shares add up to the discount exactly
 *     - package total = items total - its discount + delivery;  grand total = sum of package totals
 * </pre>
 *
 * The lesson with the worked example: Phase_4_Checkout_and_Payments/OrderSplit_Lesson_2026-10-05_1045.md
 * The records below are FIXED (CheckoutService uses them) - only the body of split(...) is yours.
 */
public final class OrderSplitter {

	public static final BigDecimal FREE_DELIVERY_FROM = new BigDecimal("499");

	public static final BigDecimal DELIVERY_FEE = new BigDecimal("40.00");

	private OrderSplitter() {
	}

	/** One cart line, with today's price (CheckoutService fills it from the catalogue). */
	public record SplitLine(Long variantId, Long sellerId, String sellerName, String productSlug, String productName,
			String variantLabel, BigDecimal unitPrice, BigDecimal mrp, int quantity) {
	}

	/** One line of a package: lineTotal = unitPrice x quantity; discountShare = its part of the coupon. */
	public record ItemPlan(SplitLine line, BigDecimal lineTotal, BigDecimal discountShare) {
	}

	/** One seller's box. total = itemsTotal - discount + shippingFee. */
	public record PackagePlan(Long sellerId, String sellerName, List<ItemPlan> items, BigDecimal itemsTotal,
			BigDecimal discount, BigDecimal shippingFee, BigDecimal total) {
	}

	/** The whole order. grandTotal = sum of the package totals. */
	public record SplitResult(List<PackagePlan> packages, BigDecimal itemsTotal, BigDecimal discountTotal,
			BigDecimal shippingTotal, BigDecimal grandTotal) {
	}

	/**
	 * @param lines    the cart lines, in cart order (at least one)
	 * @param discount the coupon discount for the whole order (0.00 when there is no coupon); never more than
	 *                 the items total (CouponService makes sure)
	 */
	public static SplitResult split(List<SplitLine> lines, BigDecimal discount) {
		// ★ Madhavan: your code goes here (see the lesson). Delete this line when you start.
		throw new UnsupportedOperationException("OrderSplitter.split is waiting for Madhavan's code");
	}

}

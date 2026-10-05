package com.trivoko.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.trivoko.common.Money;

/**
 * ★ THE ORDER SPLIT (Phase 4, step 4). Pure Java: no database, no Spring. Planned as Madhavan's code;
 * on 5 Oct 2026 he asked Claude to write it ("all") and to study it, then explain it back.
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
 * The records below are used by CheckoutService.
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
		// Rules 2 + 3: the items total of the WHOLE order (the coupon is shared against this)
		BigDecimal orderItemsTotal = BigDecimal.ZERO;
		for (SplitLine line : lines) {
			orderItemsTotal = orderItemsTotal.add(lineTotal(line));
		}

		// Rule 1: one group per seller; LinkedHashMap keeps the order in which sellers first appear
		Map<Long, List<SplitLine>> bySeller = new LinkedHashMap<>();
		for (SplitLine line : lines) {
			bySeller.computeIfAbsent(line.sellerId(), id -> new ArrayList<>()).add(line);
		}

		List<PackagePlan> packages = new ArrayList<>();
		BigDecimal sharedSoFar = BigDecimal.ZERO;
		int visited = 0;
		for (List<SplitLine> sellerLines : bySeller.values()) {
			List<ItemPlan> items = new ArrayList<>();
			BigDecimal packageItems = BigDecimal.ZERO;
			BigDecimal packageDiscount = BigDecimal.ZERO;

			for (SplitLine line : sellerLines) {
				visited++;
				BigDecimal lineTotal = lineTotal(line);
				BigDecimal share;
				if (visited == lines.size()) {
					// Rule 5: the last item of the order takes what is left, so the shares add up exactly
					share = discount.subtract(sharedSoFar);
				}
				else {
					// Rule 4: discount x lineTotal / orderItemsTotal, rounded to paise
					share = orderItemsTotal.signum() == 0 ? BigDecimal.ZERO
							: discount.multiply(lineTotal).divide(orderItemsTotal, 2, RoundingMode.HALF_UP);
				}
				sharedSoFar = sharedSoFar.add(share);
				items.add(new ItemPlan(line, lineTotal, Money.round(share)));
				packageItems = packageItems.add(lineTotal);
				packageDiscount = packageDiscount.add(share);
			}

			// Rule 7: delivery is decided on the package's items BEFORE the discount
			BigDecimal shipping = packageItems.compareTo(FREE_DELIVERY_FROM) >= 0 ? Money.ZERO : DELIVERY_FEE;
			// Rule 8
			BigDecimal total = packageItems.subtract(packageDiscount).add(shipping);
			SplitLine first = sellerLines.getFirst();
			packages.add(new PackagePlan(first.sellerId(), first.sellerName(), List.copyOf(items),
					Money.round(packageItems), Money.round(packageDiscount), shipping, Money.round(total)));
		}

		// Rule 9: the order totals are the sums of the packages
		BigDecimal itemsTotal = BigDecimal.ZERO;
		BigDecimal discountTotal = BigDecimal.ZERO;
		BigDecimal shippingTotal = BigDecimal.ZERO;
		BigDecimal grandTotal = BigDecimal.ZERO;
		for (PackagePlan p : packages) {
			itemsTotal = itemsTotal.add(p.itemsTotal());
			discountTotal = discountTotal.add(p.discount());
			shippingTotal = shippingTotal.add(p.shippingFee());
			grandTotal = grandTotal.add(p.total());
		}
		return new SplitResult(List.copyOf(packages), itemsTotal, discountTotal, shippingTotal, grandTotal);
	}

	/** Rule 2: price x quantity, in rupees with 2 decimals. */
	private static BigDecimal lineTotal(SplitLine line) {
		return Money.round(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
	}

}

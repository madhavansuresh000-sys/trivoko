package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.trivoko.order.OrderSplitter.ItemPlan;
import com.trivoko.order.OrderSplitter.PackagePlan;
import com.trivoko.order.OrderSplitter.SplitLine;
import com.trivoko.order.OrderSplitter.SplitResult;

/**
 * The order split (Phase 4, step 5). Written by Claude on 5 Oct 2026 at Madhavan's request ("all");
 * he studies it and explains it back. The first test is the worked example from the lesson.
 */
class OrderSplitterTest {

	private static SplitLine line(long variantId, long sellerId, String seller, String price, int qty) {
		return new SplitLine(variantId, sellerId, seller, "slug-" + variantId, "Product " + variantId,
				"Variant", new BigDecimal(price), new BigDecimal(price), qty);
	}

	@Test
	void twoSellersAndACoupon() {
		SplitResult r = OrderSplitter.split(List.of(
				line(1, 1, "Chennai Mobiles", "499.00", 2),
				line(2, 2, "Kovai Sports", "2499.00", 1)), new BigDecimal("349.70"));

		// two packages, in the order the sellers first appear
		assertThat(r.packages()).extracting(PackagePlan::sellerName).containsExactly("Chennai Mobiles", "Kovai Sports");

		// the coupon is shared by line total: 349.70 x 998 / 3497 = 99.80; the LAST item gets the rest (249.90)
		assertThat(r.packages().get(0).items().get(0).discountShare()).isEqualByComparingTo("99.80");
		assertThat(r.packages().get(1).items().get(0).discountShare()).isEqualByComparingTo("249.90");

		// both packages reach Rs 499 before the discount: free delivery
		assertThat(r.packages()).extracting(PackagePlan::shippingFee).allMatch(fee -> fee.signum() == 0);

		// package totals = items - discount + delivery
		assertThat(r.packages().get(0).total()).isEqualByComparingTo("898.20");
		assertThat(r.packages().get(1).total()).isEqualByComparingTo("2249.10");

		assertThat(r.itemsTotal()).isEqualByComparingTo("3497.00");
		assertThat(r.discountTotal()).isEqualByComparingTo("349.70");
		assertThat(r.grandTotal()).isEqualByComparingTo("3147.30");
	}

	@Test
	void cheapPackagePaysFortyRupeesDelivery() {
		SplitResult r = OrderSplitter.split(List.of(
				line(1, 1, "Chennai Mobiles", "200.00", 1),
				line(2, 2, "Kovai Sports", "499.00", 1)), new BigDecimal("0.00"));

		assertThat(r.packages().get(0).shippingFee()).isEqualByComparingTo("40.00"); // 200 < 499
		assertThat(r.packages().get(1).shippingFee()).isEqualByComparingTo("0.00");  // exactly 499 = free
		assertThat(r.shippingTotal()).isEqualByComparingTo("40.00");
		assertThat(r.grandTotal()).isEqualByComparingTo("739.00");
	}

	/** Delivery is decided BEFORE the discount: Rs 500 of items with Rs 50 off still ships free. */
	@Test
	void deliveryLooksAtItemsBeforeDiscount() {
		SplitResult r = OrderSplitter.split(List.of(line(1, 1, "Chennai Mobiles", "500.00", 1)), new BigDecimal("50.00"));
		assertThat(r.packages().get(0).shippingFee()).isEqualByComparingTo("0.00");
		assertThat(r.grandTotal()).isEqualByComparingTo("450.00");
	}

	/** Rounding: Rs 1 shared by three Rs 1 items = 0.33 + 0.33 + 0.34 (never 0.99). */
	@Test
	void sharesAlwaysAddUpToTheDiscount() {
		SplitResult r = OrderSplitter.split(List.of(
				line(1, 1, "Chennai Mobiles", "1.00", 1),
				line(2, 2, "Kovai Sports", "1.00", 1),
				line(3, 1, "Chennai Mobiles", "1.00", 1)), new BigDecimal("1.00"));

		BigDecimal sum = r.packages().stream().flatMap(p -> p.items().stream())
			.map(ItemPlan::discountShare).reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(sum).isEqualByComparingTo("1.00");
		// the last line VISITED is Kovai's (its package comes second), so it takes the remainder
		assertThat(r.packages().get(1).items().get(0).discountShare()).isEqualByComparingTo("0.34");
		assertThat(r.packages().get(0).discount()).isEqualByComparingTo("0.66");
	}

	@Test
	void sameSellerLinesShareOnePackage() {
		SplitResult r = OrderSplitter.split(List.of(
				line(1, 1, "Chennai Mobiles", "100.00", 2),
				line(2, 2, "Kovai Sports", "600.00", 1),
				line(3, 1, "Chennai Mobiles", "300.00", 1)), new BigDecimal("0.00"));

		assertThat(r.packages()).hasSize(2);
		assertThat(r.packages().get(0).items()).extracting(i -> i.line().variantId()).containsExactly(1L, 3L);
		assertThat(r.packages().get(0).itemsTotal()).isEqualByComparingTo("500.00");
		assertThat(r.packages().get(0).total()).isEqualByComparingTo("500.00");
	}

}

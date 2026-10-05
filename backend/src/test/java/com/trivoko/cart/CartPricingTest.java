package com.trivoko.cart;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.trivoko.cart.CartPricing.Line;
import com.trivoko.cart.dto.CartView;
import com.trivoko.catalog.dto.CartVariant;

/** The cart maths, without a database: grouping by seller, the ₹499 delivery rule, caps and notes. */
class CartPricingTest {

	private static CartVariant variant(long id, long sellerId, String price, int stock, boolean available) {
		String seller = sellerId == 1 ? "Chennai Mobiles" : "Kovai Sports";
		return new CartVariant(id, "product-" + id, "Product " + id, "Variant " + id, null, new BigDecimal(price),
				new BigDecimal(price).add(BigDecimal.TEN), stock, available, sellerId, seller,
				seller.toLowerCase().replace(' ', '-'), "Chennai");
	}

	private static Map<Long, CartVariant> catalogue(CartVariant... variants) {
		return java.util.Arrays.stream(variants).collect(java.util.stream.Collectors.toMap(CartVariant::variantId, v -> v));
	}

	@Test
	void linesAreGroupedIntoOnePackagePerSellerInTheOrderAdded() {
		CartView view = CartPricing.price(List.of(new Line(20L, 1), new Line(10L, 2), new Line(21L, 1)),
				catalogue(variant(10, 1, "499", 50, true), variant(20, 2, "2499", 50, true), variant(21, 2, "100", 50, true)));

		assertThat(view.packages()).extracting(p -> p.seller().name()).containsExactly("Kovai Sports", "Chennai Mobiles");
		assertThat(view.packages().getFirst().items()).extracting(CartView.CartLineView::variantId).containsExactly(20L, 21L);
		assertThat(view.itemCount()).isEqualTo(4);
		assertThat(view.itemsTotal()).isEqualByComparingTo("3597"); // 2499 + 100 + 2 x 499
		assertThat(view.total()).isEqualByComparingTo("3597");      // both packages >= 499: free delivery
	}

	@Test
	void deliveryIsFortyRupeesBelowFourNinetyNinePerPackage() {
		CartView below = CartPricing.price(List.of(new Line(10L, 1)), catalogue(variant(10, 1, "498", 50, true)));
		assertThat(below.packages().getFirst().shippingFee()).isEqualByComparingTo("40");
		assertThat(below.shippingTotal()).isEqualByComparingTo("40");
		assertThat(below.total()).isEqualByComparingTo("538");

		CartView exactly = CartPricing.price(List.of(new Line(10L, 1)), catalogue(variant(10, 1, "499", 50, true)));
		assertThat(exactly.packages().getFirst().shippingFee()).isEqualByComparingTo("0");
	}

	@Test
	void moreThanTenIsLoweredToTen() {
		CartView view = CartPricing.price(List.of(new Line(10L, 12)), catalogue(variant(10, 1, "100", 50, true)));
		CartView.CartLineView line = view.packages().getFirst().items().getFirst();
		assertThat(line.quantity()).isEqualTo(10);
		assertThat(line.maxQuantity()).isEqualTo(10);
		assertThat(line.note()).isEqualTo("At most 10 per item - quantity lowered");
	}

	@Test
	void lowStockLowersTheQuantityWithANote() {
		CartView view = CartPricing.price(List.of(new Line(10L, 5)), catalogue(variant(10, 1, "100", 2, true)));
		CartView.CartLineView line = view.packages().getFirst().items().getFirst();
		assertThat(line.quantity()).isEqualTo(2);
		assertThat(line.maxQuantity()).isEqualTo(2);
		assertThat(line.note()).isEqualTo("Only 2 left - quantity lowered");
		assertThat(view.itemsTotal()).isEqualByComparingTo("200");
	}

	@Test
	void soldOutLineStaysButDoesNotCount() {
		CartView view = CartPricing.price(List.of(new Line(10L, 1), new Line(11L, 3)),
				catalogue(variant(10, 1, "600", 50, true), variant(11, 1, "100", 0, true)));
		CartView.CartLineView soldOut = view.packages().getFirst().items().get(1);
		assertThat(soldOut.available()).isFalse();
		assertThat(soldOut.maxQuantity()).isZero();
		assertThat(soldOut.note()).isEqualTo("Out of stock");
		assertThat(view.itemCount()).isEqualTo(1);
		assertThat(view.itemsTotal()).isEqualByComparingTo("600");
	}

	/** Review focus: the shop was blocked (or the product taken down) while it sat in the cart. */
	@Test
	void unavailableProductIsShownButExcluded() {
		CartView view = CartPricing.price(List.of(new Line(10L, 1)), catalogue(variant(10, 1, "300", 50, false)));
		CartView.CartLineView line = view.packages().getFirst().items().getFirst();
		assertThat(line.available()).isFalse();
		assertThat(line.note()).isEqualTo("No longer available");
		assertThat(view.itemCount()).isZero();
		assertThat(view.total()).isEqualByComparingTo("0");
		assertThat(view.packages().getFirst().shippingFee()).isEqualByComparingTo("0"); // nothing to deliver
	}

	@Test
	void unknownVariantIsSkipped() {
		CartView view = CartPricing.price(List.of(new Line(99L, 1)), catalogue(variant(10, 1, "300", 50, true)));
		assertThat(view.packages()).isEmpty();
		assertThat(view.total()).isEqualByComparingTo("0");
	}

}

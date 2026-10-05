package com.trivoko.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.trivoko.cart.dto.CartView;
import com.trivoko.cart.dto.CartView.CartLineView;
import com.trivoko.cart.dto.CartView.CartPackage;
import com.trivoko.cart.dto.CartView.SellerRef;
import com.trivoko.catalog.dto.CartVariant;

/**
 * The cart maths in ONE place, used for the saved cart AND the guest preview, so both always agree.
 * Pure: no database, no Spring - it gets the lines and today's catalogue facts and returns the view.
 *
 * <pre>
 *   lines (variant, qty)  +  catalogue (price, stock, seller)
 *        -> lower qty to 10 and to the stock, mark sold-out / unavailable lines
 *        -> group by seller = packages (first seller added comes first)
 *        -> delivery per package: free from ₹499, else ₹40
 * </pre>
 */
public final class CartPricing {

	public static final int MAX_PER_LINE = 10;

	public static final BigDecimal FREE_DELIVERY_FROM = new BigDecimal("499");

	public static final BigDecimal DELIVERY_FEE = new BigDecimal("40");

	/** "Variant 57, quantity 2" - what is stored (or what the guest's browser sent). */
	public record Line(Long variantId, int quantity) {
	}

	private CartPricing() {
	}

	public static CartView price(List<Line> lines, Map<Long, CartVariant> catalogue) {
		// LinkedHashMap keeps the order in which sellers first appear
		Map<Long, List<CartLineView>> bySeller = new LinkedHashMap<>();
		Map<Long, CartVariant> sellerOf = new LinkedHashMap<>();
		for (Line line : lines) {
			CartVariant v = catalogue.get(line.variantId());
			if (v == null) {
				continue; // deleted from the catalogue: nothing to show
			}
			bySeller.computeIfAbsent(v.sellerId(), id -> new ArrayList<>()).add(lineView(line.quantity(), v));
			sellerOf.putIfAbsent(v.sellerId(), v);
		}

		List<CartPackage> packages = new ArrayList<>();
		int itemCount = 0;
		BigDecimal itemsTotal = BigDecimal.ZERO;
		BigDecimal shippingTotal = BigDecimal.ZERO;
		for (Map.Entry<Long, List<CartLineView>> entry : bySeller.entrySet()) {
			BigDecimal packageTotal = BigDecimal.ZERO;
			int packageCount = 0;
			for (CartLineView l : entry.getValue()) {
				if (l.available()) {
					packageTotal = packageTotal.add(l.price().multiply(BigDecimal.valueOf(l.quantity())));
					packageCount += l.quantity();
				}
			}
			// an empty box is not delivered, so it costs nothing
			BigDecimal fee = packageCount == 0 || packageTotal.compareTo(FREE_DELIVERY_FROM) >= 0
					? BigDecimal.ZERO : DELIVERY_FEE;
			CartVariant s = sellerOf.get(entry.getKey());
			packages.add(new CartPackage(new SellerRef(s.sellerName(), s.sellerSlug(), s.sellerCity()),
					List.copyOf(entry.getValue()), packageTotal, fee));
			itemCount += packageCount;
			itemsTotal = itemsTotal.add(packageTotal);
			shippingTotal = shippingTotal.add(fee);
		}
		return new CartView(packages, itemCount, itemsTotal, shippingTotal, itemsTotal.add(shippingTotal));
	}

	private static CartLineView lineView(int wanted, CartVariant v) {
		int quantity = Math.min(wanted, MAX_PER_LINE);
		if (!v.available()) {
			return line(v, quantity, 0, false, "No longer available");
		}
		if (v.stock() <= 0) {
			return line(v, quantity, 0, false, "Out of stock");
		}
		int max = Math.min(MAX_PER_LINE, v.stock());
		String note = null;
		if (wanted > max) {
			note = max < MAX_PER_LINE ? "Only " + max + " left - quantity lowered" : "At most 10 per item - quantity lowered";
		}
		return line(v, Math.min(quantity, max), max, true, note);
	}

	private static CartLineView line(CartVariant v, int quantity, int max, boolean available, String note) {
		return new CartLineView(v.variantId(), v.productSlug(), v.productName(), v.variantLabel(), v.imageUrl(),
				v.price(), v.mrp(), quantity, max, available, note);
	}

}

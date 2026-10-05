package com.trivoko.cart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.cart.CartPricing.Line;
import com.trivoko.cart.dto.CartItemsRequest;
import com.trivoko.cart.dto.CartView;
import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.CartVariant;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * The cart's rules. A logged-in user's cart is saved here; a guest's cart is only priced (preview) and
 * merged after login. Every answer is a fresh CartView, so the page always shows today's prices and stock.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

	static final int MAX_LINES = 30;

	private final CartRepository carts;

	private final ProductService productService;

	public CartView view(Long userId) {
		return carts.findByUserId(userId).map(this::price).orElseGet(() -> CartPricing.price(List.of(), Map.of()));
	}

	/** Sets a line's quantity (adds the line if it is new). Stored at most the stock, so the note does not stick. */
	@Transactional
	public CartView setQuantity(Long userId, Long variantId, int quantity) {
		CartVariant variant = productService.cartVariants(List.of(variantId)).get(variantId);
		if (variant == null) {
			throw new ResourceNotFoundException("Variant", variantId);
		}
		Cart cart = cartOf(userId);
		int stored = lowerToStock(quantity, variant);
		cart.line(variantId).ifPresentOrElse(line -> line.setQuantity(stored), () -> {
			// a line already in the cart may stay (shown "Out of stock"), but nobody can ADD what cannot be bought
			if (!variant.available() || variant.stock() <= 0) {
				throw new BusinessRuleException("Sorry, " + variant.productName() + " (" + variant.variantLabel()
						+ ") cannot be bought right now.");
			}
			if (cart.getItems().size() >= MAX_LINES) {
				throw new BusinessRuleException("A cart can hold at most " + MAX_LINES + " different items.");
			}
			cart.add(variantId, stored);
		});
		return price(cart);
	}

	/** Removing a line that is not there is fine (the button was pressed twice). */
	@Transactional
	public CartView remove(Long userId, Long variantId) {
		return carts.findByUserId(userId).map(cart -> {
			cart.remove(variantId);
			return price(cart);
		}).orElseGet(() -> view(userId));
	}

	/**
	 * After login: the guest's lines are added to the saved cart. The same variant in both = quantities
	 * added (then lowered to 10 / the stock). Unknown variants and lines beyond 30 are skipped quietly -
	 * the guest's browser copy may be old, and the login must not fail because of it.
	 */
	@Transactional
	public CartView merge(Long userId, CartItemsRequest request) {
		Map<Long, Integer> guest = combine(request);
		if (guest.isEmpty()) {
			return view(userId);
		}
		Map<Long, CartVariant> catalogue = productService.cartVariants(guest.keySet());
		Cart cart = cartOf(userId);
		guest.forEach((variantId, quantity) -> {
			CartVariant variant = catalogue.get(variantId);
			if (variant == null) {
				return;
			}
			cart.line(variantId).ifPresentOrElse(
					line -> line.setQuantity(lowerToStock(line.getQuantity() + quantity, variant)),
					() -> {
						if (cart.getItems().size() < MAX_LINES) {
							cart.add(variantId, lowerToStock(quantity, variant));
						}
					});
		});
		return price(cart);
	}

	/** A guest's cart, priced with the same rules - nothing is saved. */
	public CartView preview(CartItemsRequest request) {
		List<Line> lines = combine(request).entrySet().stream()
			.limit(MAX_LINES)
			.map(e -> new Line(e.getKey(), e.getValue()))
			.toList();
		return CartPricing.price(lines, productService.cartVariants(lines.stream().map(Line::variantId).toList()));
	}

	/** Checkout (order module): my saved lines, oldest first - no prices (the order module prices them). */
	public List<Line> checkoutLines(Long userId) {
		return carts.findByUserId(userId)
			.map(cart -> cart.getItems().stream().map(i -> new Line(i.getVariantId(), i.getQuantity())).toList())
			.orElse(List.of());
	}

	/** The order was PAID: what was bought leaves the cart; anything else stays for next time. */
	@Transactional
	public void removeVariants(Long userId, java.util.Collection<Long> variantIds) {
		carts.findByUserId(userId).ifPresent(cart -> variantIds.forEach(cart::remove));
	}

	private CartView price(Cart cart) {
		List<Line> lines = cart.getItems().stream().map(i -> new Line(i.getVariantId(), i.getQuantity())).toList();
		return CartPricing.price(lines, productService.cartVariants(lines.stream().map(Line::variantId).toList()));
	}

	private Cart cartOf(Long userId) {
		return carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(userId)));
	}

	/** The same variant sent twice = one line with both quantities (order of first appearance kept). */
	private static Map<Long, Integer> combine(CartItemsRequest request) {
		Map<Long, Integer> lines = new LinkedHashMap<>();
		request.items().forEach(i -> lines.merge(i.variantId(), i.quantity(), Integer::sum));
		return lines;
	}

	/** 1..10, and not above the stock - except a sold-out line keeps 1 so it can still be shown "Out of stock". */
	private static int lowerToStock(int quantity, CartVariant variant) {
		int max = Math.max(1, Math.min(CartPricing.MAX_PER_LINE, variant.stock()));
		return Math.clamp(quantity, 1, max);
	}

}

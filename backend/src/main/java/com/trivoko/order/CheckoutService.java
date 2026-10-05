package com.trivoko.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.cart.CartPricing;
import com.trivoko.cart.CartService;
import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.CartVariant;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.Money;
import com.trivoko.coupon.CouponService;
import com.trivoko.coupon.CouponService.CouponQuote;
import com.trivoko.order.OrderSplitter.ItemPlan;
import com.trivoko.order.OrderSplitter.PackagePlan;
import com.trivoko.order.OrderSplitter.SplitLine;
import com.trivoko.order.OrderSplitter.SplitResult;
import com.trivoko.order.dto.CheckoutDtos.CheckoutPreview;
import com.trivoko.order.dto.CheckoutDtos.CouponResult;
import com.trivoko.order.dto.CheckoutDtos.ItemView;
import com.trivoko.order.dto.CheckoutDtos.PackageView;
import com.trivoko.order.dto.CheckoutDtos.PlaceOrderRequest;
import com.trivoko.order.dto.CheckoutDtos.PlacedOrder;
import com.trivoko.order.dto.CheckoutDtos.PreviewRequest;
import com.trivoko.payment.PaymentGateway.CheckoutSession;
import com.trivoko.payment.PaymentService;
import com.trivoko.user.AddressService;
import com.trivoko.user.UserService;
import com.trivoko.user.dto.AddressView;

/**
 * Checkout (spec section 2). The server never trusts the browser's numbers: it reads the cart lines, today's
 * prices and the coupon itself, splits the order (OrderSplitter), and only then holds stock and opens payment.
 *
 * <pre>
 *   preview: cart -> today's prices -> coupon -> OrderSplitter            (nothing saved)
 *   place  : same + "total = what you saw?" -> hold stock -> order + packages + items -> payment page
 *            (one transaction: any failure undoes every step)
 * </pre>
 */
@Service
@Transactional(readOnly = true)
public class CheckoutService {

	private final CartService cartService;

	private final ProductService productService;

	private final CouponService couponService;

	private final AddressService addressService;

	private final UserService userService;

	private final PaymentService paymentService;

	private final OrderRepository orders;

	private final OrderNumbers numbers;

	private final OrderService orderService;

	private final Duration holdTime;

	CheckoutService(CartService cartService, ProductService productService, CouponService couponService,
			AddressService addressService, UserService userService, PaymentService paymentService,
			OrderRepository orders, OrderNumbers numbers, OrderService orderService,
			@Value("${app.order.hold-time}") Duration holdTime) {
		this.cartService = cartService;
		this.productService = productService;
		this.couponService = couponService;
		this.addressService = addressService;
		this.userService = userService;
		this.paymentService = paymentService;
		this.orders = orders;
		this.numbers = numbers;
		this.orderService = orderService;
		this.holdTime = holdTime;
	}

	public CheckoutPreview preview(Long userId, PreviewRequest request) {
		AddressView address = request.addressId() != null
				? addressService.get(userId, request.addressId())
				: addressService.defaultOf(userId).orElse(null);
		return build(userId, request.couponCode(), address).preview();
	}

	/** POST /api/orders. Returns where to pay. */
	@Transactional
	public PlacedOrder place(Long userId, PlaceOrderRequest request) {
		AddressView address = addressService.get(userId, request.addressId()); // someone else's -> 404

		// one unpaid order at a time. The SAME order again (another tab, a double click) goes back to its page;
		// a CHANGED cart, coupon or address replaces it (its stock and coupon are freed first), so nobody ever
		// pays for an old version of their cart.
		var pending = orders.findFirstByUserIdAndStatusOrderByIdDesc(userId, OrderStatus.PENDING_PAYMENT);
		if (pending.isPresent()) {
			Order order = pending.get();
			if (order.getHoldExpiresAt().isAfter(LocalDateTime.now()) && sameAs(order, userId, request.couponCode(), address)) {
				String page = paymentService.openPageOf(order.getId()).orElse(null);
				if (page != null) {
					return new PlacedOrder(order.getNumber(), order.getGrandTotal(), page);
				}
			}
			orderService.expire(order); // out of time, or replaced by this new checkout
		}

		Built built = build(userId, request.couponCode(), address);
		if (built.split() == null) {
			throw new BusinessRuleException(built.preview().unavailable().isEmpty()
					? "Your cart is empty."
					: "Nothing in your cart can be bought right now.");
		}
		CouponResult coupon = built.preview().coupon();
		if (coupon != null && coupon.error() != null) {
			throw new BusinessRuleException(coupon.error());
		}
		SplitResult split = built.split();
		if (split.grandTotal().compareTo(request.expectedTotal()) != 0) {
			throw new BusinessRuleException("Prices changed since you looked: the total is now ₹"
					+ split.grandTotal().toPlainString() + ". Please check your order again.");
		}

		// hold the stock of every line, or none (throws "only N left" and rolls everything back)
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		split.packages().forEach(p -> p.items().forEach(i -> quantities.merge(i.line().variantId(), i.line().quantity(), Integer::sum)));
		productService.holdStock(quantities);

		Order order = new Order(numbers.next(), userId, split.itemsTotal(), split.discountTotal(), split.shippingTotal(),
				split.grandTotal(), coupon == null ? null : coupon.code(), shipTo(address),
				LocalDateTime.now().plus(holdTime));
		for (PackagePlan p : split.packages()) {
			OrderPackage box = new OrderPackage(p.sellerId(), p.sellerName(), p.itemsTotal(), p.discount(),
					p.shippingFee(), p.total());
			for (ItemPlan i : p.items()) {
				SplitLine l = i.line();
				box.addItem(new OrderItem(l.variantId(), l.productSlug(), l.productName(), l.variantLabel(), l.unitPrice(),
						l.mrp(), l.quantity(), i.lineTotal(), i.discountShare()));
			}
			order.addPackage(box);
		}
		orders.save(order);

		int count = quantities.values().stream().mapToInt(Integer::intValue).sum();
		CheckoutSession session = paymentService.start(order.getId(), order.getNumber(),
				userService.getById(userId).getEmail(), order.getGrandTotal(),
				"TriVoKo order " + order.getNumber() + " (" + count + (count == 1 ? " item" : " items") + ", "
						+ split.packages().size() + (split.packages().size() == 1 ? " package)" : " packages)"));
		return new PlacedOrder(order.getNumber(), order.getGrandTotal(), session.redirectUrl());
	}

	/** The checkout picture: buyable lines split into packages; lines that cannot be bought are listed by name. */
	private Built build(Long userId, String couponCode, AddressView address) {
		List<CartPricing.Line> cart = cartService.checkoutLines(userId);
		Map<Long, CartVariant> catalogue = productService.cartVariants(cart.stream().map(CartPricing.Line::variantId).toList());

		List<SplitLine> lines = new ArrayList<>();
		List<String> unavailable = new ArrayList<>();
		for (CartPricing.Line c : cart) {
			CartVariant v = catalogue.get(c.variantId());
			if (v == null) {
				continue;
			}
			if (!v.available() || v.stock() <= 0) {
				unavailable.add(v.productName() + " (" + v.variantLabel() + ")");
				continue;
			}
			int quantity = Math.min(c.quantity(), Math.min(CartPricing.MAX_PER_LINE, v.stock()));
			lines.add(new SplitLine(v.variantId(), v.sellerId(), v.sellerName(), v.productSlug(), v.productName(),
					v.variantLabel(), v.price(), v.mrp(), quantity));
		}
		if (lines.isEmpty()) {
			return new Built(new CheckoutPreview(address, List.of(), Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO,
					null, unavailable), null);
		}

		BigDecimal itemsTotal = lines.stream().map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		CouponResult coupon = null;
		BigDecimal discount = Money.ZERO;
		if (couponCode != null && !couponCode.isBlank()) {
			String code = couponCode.trim().toUpperCase(java.util.Locale.ROOT);
			boolean inPendingOrder = orders.existsByUserIdAndStatusAndCouponCode(userId, OrderStatus.PENDING_PAYMENT, code);
			CouponQuote quote = couponService.quote(code, userId, itemsTotal, inPendingOrder);
			coupon = new CouponResult(quote.code(), quote.discount(), quote.error());
			discount = quote.discount();
		}

		SplitResult split = OrderSplitter.split(lines, discount);
		Map<Long, String> slugOf = new LinkedHashMap<>();
		catalogue.values().forEach(v -> slugOf.putIfAbsent(v.sellerId(), v.sellerSlug()));
		List<PackageView> packages = split.packages().stream().map(p -> new PackageView(p.sellerName(),
				slugOf.get(p.sellerId()), p.items().stream().map(i -> new ItemView(i.line().variantId(),
						i.line().productSlug(), i.line().productName(), i.line().variantLabel(), i.line().unitPrice(),
						i.line().mrp(), i.line().quantity(), i.lineTotal(), i.discountShare())).toList(),
				p.itemsTotal(), p.discount(), p.shippingFee(), p.total())).toList();
		return new Built(new CheckoutPreview(address, packages, split.itemsTotal(), split.discountTotal(),
				split.shippingTotal(), split.grandTotal(), coupon, unavailable), split);
	}

	/** Is this checkout the same as the unpaid order: same cart lines and quantities, coupon and address? */
	private boolean sameAs(Order order, Long userId, String couponCode, AddressView address) {
		Map<Long, Integer> ordered = new LinkedHashMap<>();
		order.getPackages().forEach(p -> p.getItems().forEach(i -> ordered.merge(i.getVariantId(), i.getQuantity(), Integer::sum)));
		Map<Long, Integer> inCart = new LinkedHashMap<>();
		cartService.checkoutLines(userId).forEach(l -> inCart.merge(l.variantId(), l.quantity(), Integer::sum));
		String code = couponCode == null || couponCode.isBlank() ? null : couponCode.trim().toUpperCase(java.util.Locale.ROOT);
		return ordered.equals(inCart) && java.util.Objects.equals(code, order.getCouponCode())
				&& order.shipTo().equals(shipTo(address));
	}

	private static Order.ShipTo shipTo(AddressView a) {
		return new Order.ShipTo(a.name(), a.phone(), a.line1(), a.line2(), a.city(), a.state(), a.pincode());
	}

	/** The preview, plus the split behind it (null when nothing can be bought). */
	private record Built(CheckoutPreview preview, SplitResult split) {
	}

}

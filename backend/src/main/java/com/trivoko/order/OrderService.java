package com.trivoko.order;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.cart.CartService;
import com.trivoko.catalog.ProductService;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.coupon.CouponService;
import com.trivoko.order.dto.OrderViews;
import com.trivoko.order.dto.OrderViews.OrderDetail;
import com.trivoko.order.dto.OrderViews.OrderSummary;
import com.trivoko.payment.Payment;
import com.trivoko.payment.PaymentService;

import lombok.RequiredArgsConstructor;

/**
 * What happens to an order after "Place order" (spec section 2):
 *
 * <pre>
 *   payment confirmed (webhook / verify / fake page) -> PAID, packages PLACED, coupon used, bought lines leave the cart
 *   10 minutes, not paid (job)                      -> EXPIRED, stock back, payment page closed
 *   paid LATE (already EXPIRED)                      -> take the stock again if it is all still there -> PAID,
 *                                                      otherwise give the money back (same rule as EventHub)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orders;

	private final ProductService productService;

	private final PaymentService paymentService;

	private final CouponService couponService;

	private final CartService cartService;

	private final OrderNotifications notifications;

	/** What happened to a payment message - for logs and tests. */
	public enum PaidResult {
		PAID, ALREADY_DONE, PAID_LATE, REFUNDED
	}

	/**
	 * A payment was confirmed. eventId makes it idempotent: a webhook sent twice changes the order once.
	 * (The fake page and "verify" use "session:" + sessionId as their event id, so they share the guard.)
	 */
	@Transactional
	public PaidResult paymentReceived(String eventId, String sessionId) {
		if (!paymentService.firstTime(eventId)) {
			return PaidResult.ALREADY_DONE;
		}
		Payment payment = paymentService.bySession(sessionId);
		Order order = orders.findById(payment.getOrderId())
			.orElseThrow(() -> new ResourceNotFoundException("Order", payment.getOrderId()));
		switch (order.getStatus()) {
			case PAID -> {
				return PaidResult.ALREADY_DONE;
			}
			case PENDING_PAYMENT -> {
				markPaid(order, sessionId);
				return PaidResult.PAID;
			}
			case EXPIRED -> {
				// paid after the 10 minutes: the stock went back on the shelf - is it all still there?
				if (!productService.tryHoldStock(quantities(order))) {
					log.info("Order {} paid late and the stock is gone: refunding", order.getNumber());
					paymentService.refund(sessionId);
					return PaidResult.REFUNDED;
				}
				markPaid(order, sessionId);
				return PaidResult.PAID_LATE;
			}
			default -> throw new IllegalStateException("Unknown order status " + order.getStatus());
		}
	}

	private void markPaid(Order order, String sessionId) {
		order.setStatus(OrderStatus.PAID);
		order.setPaidAt(LocalDateTime.now());
		order.getPackages().forEach(p -> p.setStatus(PackageStatus.PLACED));
		paymentService.markPaid(sessionId);
		if (order.getCouponCode() != null) {
			couponService.redeem(order.getCouponCode(), order.getUserId(), order.getId());
		}
		Set<Long> bought = order.getPackages().stream().flatMap(p -> p.getItems().stream())
			.map(OrderItem::getVariantId).collect(Collectors.toSet());
		cartService.removeVariants(order.getUserId(), bought);
		notifications.paid(order); // bell now, emails after the commit
		log.info("Order {} PAID: {} packages PLACED", order.getNumber(), order.getPackages().size());
	}

	/** The job (every minute): unpaid orders whose 10 minutes are over. Returns how many expired. */
	@Transactional
	public int expireOldHolds() {
		List<Order> old = orders.findByStatusAndHoldExpiresAtBefore(OrderStatus.PENDING_PAYMENT, LocalDateTime.now());
		old.forEach(this::expire);
		return old.size();
	}

	/** Not paid in time: stock back on the shelf, packages EXPIRED, the payment page closed. */
	@Transactional
	public void expire(Order order) {
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			return;
		}
		order.setStatus(OrderStatus.EXPIRED);
		order.getPackages().forEach(p -> p.setStatus(PackageStatus.EXPIRED));
		productService.releaseStock(quantities(order));
		paymentService.expireOpenPages(order.getId());
		log.info("Order {} EXPIRED (not paid in time), stock released", order.getNumber());
	}

	// ---------- the customer's own orders ----------

	public PageResponse<OrderSummary> mine(Long userId, Pageable pageable) {
		return PageResponse.from(orders.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable), OrderViews::summary);
	}

	/** Someone else's order number is simply "not found" (404), never "forbidden" (do not confirm it exists). */
	public OrderDetail mine(Long userId, String number) {
		return OrderViews.detail(owned(userId, number));
	}

	Order owned(Long userId, String number) {
		return orders.findByNumber(number).filter(o -> o.getUserId().equals(userId))
			.orElseThrow(() -> new ResourceNotFoundException("Order", number));
	}

	/** The fake test page and "verify": the session must belong to one of MY orders. */
	public Payment paymentOfMine(Long userId, String sessionId) {
		Payment payment = paymentService.bySession(sessionId);
		orders.findById(payment.getOrderId()).filter(o -> o.getUserId().equals(userId))
			.orElseThrow(() -> new ResourceNotFoundException("Payment", sessionId));
		return payment;
	}

	/** "Verify on return": has the payment company seen the money? */
	public boolean paidAtProvider(String sessionId) {
		return paymentService.confirmedByProvider(sessionId);
	}

	public String numberOf(Long orderId) {
		return orders.findById(orderId).map(Order::getNumber).orElseThrow();
	}

	private static Map<Long, Integer> quantities(Order order) {
		Map<Long, Integer> q = new LinkedHashMap<>();
		order.getPackages().forEach(p -> p.getItems().forEach(i -> q.merge(i.getVariantId(), i.getQuantity(), Integer::sum)));
		return q;
	}

}

package com.trivoko.order;

import java.math.BigDecimal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.order.dto.OrderViews.OrderDetail;
import com.trivoko.payment.Payment;
import com.trivoko.payment.PaymentProvider;
import com.trivoko.payment.PaymentStatus;

import lombok.RequiredArgsConstructor;

/**
 * The customer's side of paying (logged in, own orders only):
 *   GET  /api/payments/fake/{session}            the built-in TEST payment page (no Stripe key)
 *   POST /api/payments/fake/{session}/complete   its "Pay" button - runs the SAME code as a real webhook
 *   POST /api/payments/{session}/verify          back from Stripe: ask Stripe directly (the webhook may be late)
 * Lives in the order module because it changes orders (the payment module never calls the order module).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentPagesController {

	private final OrderService orderService;

	/** What the test page shows. */
	public record TestPayment(String orderNumber, BigDecimal amount, PaymentStatus status) {
	}

	@GetMapping("/fake/{sessionId}")
	public TestPayment testPage(@AuthenticationPrincipal AuthUser user, @PathVariable String sessionId) {
		Payment p = fakeOfMine(user, sessionId);
		return new TestPayment(orderService.numberOf(p.getOrderId()), p.getAmount(), p.getStatus());
	}

	@PostMapping("/fake/{sessionId}/complete")
	public OrderDetail completeTestPayment(@AuthenticationPrincipal AuthUser user, @PathVariable String sessionId) {
		Payment p = fakeOfMine(user, sessionId);
		orderService.paymentReceived("session:" + sessionId, sessionId);
		return orderService.mine(user.id(), orderService.numberOf(p.getOrderId()));
	}

	@PostMapping("/{sessionId}/verify")
	public OrderDetail verify(@AuthenticationPrincipal AuthUser user, @PathVariable String sessionId) {
		Payment p = orderService.paymentOfMine(user.id(), sessionId);
		if (p.getStatus() == PaymentStatus.CREATED && orderService.paidAtProvider(sessionId)) {
			orderService.paymentReceived("session:" + sessionId, sessionId);
		}
		return orderService.mine(user.id(), orderService.numberOf(p.getOrderId()));
	}

	/** Only for the test page: a real Stripe session cannot be "paid" by pressing our button. */
	private Payment fakeOfMine(AuthUser user, String sessionId) {
		Payment p = orderService.paymentOfMine(user.id(), sessionId);
		if (p.getProvider() != PaymentProvider.FAKE) {
			throw new ResourceNotFoundException("Payment", sessionId);
		}
		return p;
	}

}

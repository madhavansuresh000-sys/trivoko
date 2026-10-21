package com.trivoko.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.payment.PaymentGateway.CheckoutSession;
import com.trivoko.payment.PaymentGateway.OrderPayment;

import lombok.RequiredArgsConstructor;

/**
 * The payment module's counter. It knows payment pages and payment messages; the ORDER module decides what
 * a payment means for the order (it calls these methods inside its own transaction).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private final PaymentGateway gateway;

	private final PaymentRepository payments;

	private final ProcessedPaymentEventRepository processedEvents;

	/** Opens a payment page for the order and remembers it (status CREATED). */
	@Transactional
	public CheckoutSession start(Long orderId, String orderNumber, String email, BigDecimal amount, String description) {
		CheckoutSession session = gateway.createCheckout(new OrderPayment(orderNumber, email, amount, description));
		payments.save(new Payment(orderId, gateway.provider(), session.sessionId(), amount));
		return session;
	}

	/** The payment page of an unpaid order (a second "Pay" click goes back to the same page). */
	public Optional<String> openPageOf(Long orderId) {
		return payments.findFirstByOrderIdOrderByIdDesc(orderId)
			.filter(p -> p.getStatus() == PaymentStatus.CREATED)
			.map(p -> gateway.redirectUrl(p.getSessionId()));
	}

	/** Which order does this payment page belong to? (404 for an unknown session.) */
	public Payment bySession(String sessionId) {
		return payments.findBySessionId(sessionId)
			.orElseThrow(() -> new ResourceNotFoundException("Payment", sessionId));
	}

	/**
	 * Idempotency: true the FIRST time a message id is seen, false every time after (a webhook sent twice).
	 * The event table's primary key makes this safe even when two copies arrive at the same moment.
	 */
	@Transactional
	public boolean firstTime(String eventId) {
		if (processedEvents.existsById(eventId)) {
			return false;
		}
		processedEvents.saveAndFlush(new ProcessedPaymentEvent(eventId));
		return true;
	}

	@Transactional
	public void markPaid(String sessionId) {
		Payment p = bySession(sessionId);
		p.setStatus(PaymentStatus.PAID);
		p.setPaidAt(LocalDateTime.now());
	}

	/** Paid too late and the stock is gone: the money goes back. */
	@Transactional
	public void refund(String sessionId) {
		gateway.refund(sessionId);
		bySession(sessionId).setStatus(PaymentStatus.REFUNDED);
	}

	/** The 10 minutes are over: close the page. */
	@Transactional
	public void expireOpenPages(Long orderId) {
		payments.findFirstByOrderIdOrderByIdDesc(orderId)
			.filter(p -> p.getStatus() == PaymentStatus.CREATED)
			.ifPresent(p -> {
				gateway.expireSession(p.getSessionId());
				p.setStatus(PaymentStatus.EXPIRED);
			});
	}

	/** Back from the payment page: ask the payment company directly (the webhook may be late). */
	public boolean confirmedByProvider(String sessionId) {
		return gateway.isPaid(sessionId);
	}

	public PaymentProvider provider() {
		return gateway.provider();
	}

}

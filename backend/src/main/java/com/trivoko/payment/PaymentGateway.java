package com.trivoko.payment;

import java.math.BigDecimal;

/**
 * The outside payment company, behind one small interface (like a plug socket - copied from EventHub):
 *   StripePaymentGateway - real Stripe Checkout in test mode (when STRIPE_SECRET_KEY is in .env)  [Phase 4B]
 *   FakePaymentGateway   - a built-in test page for development (no keys needed)
 * The order code only talks to this interface, so it works the same with both.
 */
public interface PaymentGateway {

	PaymentProvider provider();

	/** Opens a payment page for the whole order. The customer is sent to redirectUrl to pay. */
	CheckoutSession createCheckout(OrderPayment payment);

	/** The page of an existing session (a second "Pay" click on the same unpaid order goes back to it). */
	String redirectUrl(String sessionId);

	/** Has the customer paid? (asked when they come back, in case the webhook is late) */
	boolean isPaid(String sessionId);

	/** The 10 minutes are over: close the page so nobody can pay any more. Best effort. */
	void expireSession(String sessionId);

	/** Gives the money back (a payment that arrived after the stock was gone). */
	void refund(String sessionId);

	/** What to charge: one amount for the whole order (coupon and delivery already inside). */
	record OrderPayment(String orderNumber, String customerEmail, BigDecimal amount, String description) {
	}

	record CheckoutSession(String sessionId, String redirectUrl) {
	}

}

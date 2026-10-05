package com.trivoko.payment;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DEVELOPMENT ONLY: pretends to be a payment company (copied from EventHub). The "payment page" is a page of
 * our own React app (/test-payment/{session}); its Pay button calls POST /api/payments/fake/{session}/complete,
 * which runs the SAME confirm code as a real Stripe webhook. Never used in prod (see PaymentConfig).
 */
public class FakePaymentGateway implements PaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

	private final String frontendUrl;

	public FakePaymentGateway(String frontendUrl) {
		this.frontendUrl = frontendUrl;
	}

	@Override
	public PaymentProvider provider() {
		return PaymentProvider.FAKE;
	}

	@Override
	public CheckoutSession createCheckout(OrderPayment payment) {
		String sessionId = "fake_cs_" + UUID.randomUUID().toString().replace("-", "");
		return new CheckoutSession(sessionId, redirectUrl(sessionId));
	}

	@Override
	public String redirectUrl(String sessionId) {
		return frontendUrl + "/test-payment/" + sessionId;
	}

	/** The fake page reports payments itself (the /complete call), so there is nothing to ask. */
	@Override
	public boolean isPaid(String sessionId) {
		return false;
	}

	@Override
	public void expireSession(String sessionId) {
		log.info("[fake payments] session {} closed", sessionId);
	}

	@Override
	public void refund(String sessionId) {
		log.info("[fake payments] money for session {} given back", sessionId);
	}

}

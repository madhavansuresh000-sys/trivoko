package com.trivoko.payment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.trivoko.common.Money;

/**
 * Real Stripe Checkout (use TEST keys: sk_test_..., so no real money moves). Copied from EventHub.
 * The customer pays on Stripe's own page; card numbers never touch our server.
 * The whole order is ONE line ("TriVoKo order TV-100123") for the grand total: coupon shares and delivery
 * are already inside that amount, so Stripe charges exactly what our order says.
 */
public class StripePaymentGateway implements PaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

	/** Stripe does not let a checkout page close sooner than 30 minutes; our expiry job closes it at 10. */
	private static final long SESSION_MINUTES = 31;

	private final String secretKey;

	private final RequestOptions options;

	private final String frontendUrl;

	public StripePaymentGateway(String secretKey, String frontendUrl) {
		this.secretKey = secretKey;
		this.options = RequestOptions.builder().setApiKey(secretKey).build();
		this.frontendUrl = frontendUrl;
	}

	@Override
	public PaymentProvider provider() {
		return PaymentProvider.STRIPE;
	}

	@Override
	public CheckoutSession createCheckout(OrderPayment payment) {
		SessionCreateParams params = SessionCreateParams.builder()
			.setMode(SessionCreateParams.Mode.PAYMENT)
			.setClientReferenceId(payment.orderNumber())
			.putMetadata("order_number", payment.orderNumber())
			.setCustomerEmail(payment.customerEmail())
			.setExpiresAt(Instant.now().plus(SESSION_MINUTES, ChronoUnit.MINUTES).getEpochSecond())
			.setSuccessUrl(frontendUrl + "/payment/success?order=" + payment.orderNumber() + "&session={CHECKOUT_SESSION_ID}")
			.setCancelUrl(frontendUrl + "/payment/cancelled?order=" + payment.orderNumber())
			.addLineItem(SessionCreateParams.LineItem.builder()
				.setQuantity(1L)
				.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
					.setCurrency("inr")
					.setUnitAmount(Money.toPaise(payment.amount())) // Stripe counts in paise: Rs 1 = 100
					.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
						.setName(payment.description())
						.build())
					.build())
				.build())
			.build();
		try {
			Session session = Session.create(params, options);
			return new CheckoutSession(session.getId(), session.getUrl());
		}
		catch (StripeException ex) {
			throw new PaymentProviderException("Could not start the payment. Please try again.", ex);
		}
	}

	@Override
	public String redirectUrl(String sessionId) {
		try {
			return Session.retrieve(sessionId, options).getUrl();
		}
		catch (StripeException ex) {
			throw new PaymentProviderException("Could not open the payment page. Please try again.", ex);
		}
	}

	@Override
	public boolean isPaid(String sessionId) {
		try {
			return "paid".equals(Session.retrieve(sessionId, options).getPaymentStatus());
		}
		catch (StripeException ex) {
			throw new PaymentProviderException("Could not check the payment with Stripe. Please try again.", ex);
		}
	}

	@Override
	public void expireSession(String sessionId) {
		try {
			Session.retrieve(sessionId, options).expire(options);
		}
		catch (StripeException ex) {
			// e.g. already paid or already expired: nothing to close
			log.info("Stripe session {} not expired: {}", sessionId, ex.getMessage());
		}
	}

	@Override
	public void refund(String sessionId) {
		try {
			String paymentIntent = Session.retrieve(sessionId, options).getPaymentIntent();
			// the idempotency key: a retried refund call can never give the money back twice
			Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentIntent).build(),
					RequestOptions.builder().setApiKey(secretKey).setIdempotencyKey("refund-" + sessionId).build());
		}
		catch (StripeException ex) {
			throw new PaymentProviderException("Could not refund the payment. Please contact TriVoKo support.", ex);
		}
	}

}

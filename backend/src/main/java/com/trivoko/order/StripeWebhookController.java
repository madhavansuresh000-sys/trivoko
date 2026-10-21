package com.trivoko.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

/**
 * Stripe's server tells us a checkout was paid (copied from EventHub). Anyone on the internet can call this
 * URL, so we FIRST check the Stripe-Signature header: an HMAC made with the webhook secret that only Stripe
 * and we know. Wrong signature -> 400 and nothing changes. Every genuine message gets 200 quickly - also a
 * duplicate - otherwise Stripe sends it again. No login and no CSRF here (SecurityConfig): the signature is
 * the proof. Lives in the order module because it changes orders.
 */
@RestController
public class StripeWebhookController {

	private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

	private final OrderService orders;

	private final String webhookSecret;

	StripeWebhookController(OrderService orders, @Value("${app.stripe.webhook-secret:}") String webhookSecret) {
		this.orders = orders;
		this.webhookSecret = webhookSecret;
	}

	@PostMapping("/api/payments/stripe/webhook")
	public ResponseEntity<String> webhook(@RequestBody String payload,
			@RequestHeader(name = "Stripe-Signature", required = false) String signature) {
		if (webhookSecret.isBlank()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Stripe webhook secret is not configured");
		}
		Event event;
		try {
			event = Webhook.constructEvent(payload, signature, webhookSecret);
		}
		catch (SignatureVerificationException | RuntimeException ex) {
			log.warn("Rejected a webhook with a bad signature");
			return ResponseEntity.badRequest().body("Invalid signature");
		}

		String type = event.getType();
		if ("checkout.session.completed".equals(type) || "checkout.session.async_payment_succeeded".equals(type)) {
			Session session = (Session) dataObject(event);
			if ("paid".equals(session.getPaymentStatus())) {
				var result = orders.paymentReceived(event.getId(), session.getId());
				log.info("Stripe {} for session {}: {}", type, session.getId(), result);
			}
		}
		return ResponseEntity.ok("received");
	}

	/** Stripe may send data in a newer format than this library knows; the fields we read still work. */
	private static StripeObject dataObject(Event event) {
		var data = event.getDataObjectDeserializer();
		return data.getObject().orElseGet(() -> {
			try {
				return data.deserializeUnsafe();
			}
			catch (EventDataObjectDeserializationException ex) {
				throw new IllegalStateException("Cannot read Stripe event " + event.getId(), ex);
			}
		});
	}

}

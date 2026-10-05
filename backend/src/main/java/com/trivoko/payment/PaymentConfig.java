package com.trivoko.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Picks the payment gateway (copied from EventHub):
 *   STRIPE_SECRET_KEY set   -> real Stripe (test mode)
 *   not set (dev and tests) -> the built-in test payment page
 * On the real server (prod) a key is required - unless PAYMENTS_TEST_PAGE=true, which a public DEMO server
 * may set on purpose (the page says "no real money").
 */
@Configuration
public class PaymentConfig {

	private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);

	@Bean
	PaymentGateway paymentGateway(@Value("${app.stripe.secret-key:}") String secretKey,
			@Value("${app.frontend-url}") String frontendUrl,
			@Value("${app.payments.test-page-allowed:false}") boolean testPageAllowed, Environment env) {
		if (!secretKey.isBlank()) {
			log.info("Payments: Stripe ({} mode)", secretKey.startsWith("sk_live_") ? "LIVE" : "test");
			return new StripePaymentGateway(secretKey, frontendUrl);
		}
		if (env.acceptsProfiles(Profiles.of("prod")) && !testPageAllowed) {
			throw new IllegalStateException("STRIPE_SECRET_KEY is required in prod (or PAYMENTS_TEST_PAGE=true for a demo server)");
		}
		log.warn("Payments: built-in TEST payment page (no STRIPE_SECRET_KEY in .env) - development only");
		return new FakePaymentGateway(frontendUrl);
	}

}

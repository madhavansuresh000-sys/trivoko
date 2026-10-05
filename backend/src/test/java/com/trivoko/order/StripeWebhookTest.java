package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.stripe.net.Webhook;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * POST /api/payments/stripe/webhook. Anyone on the internet can call it, so the Stripe-Signature header
 * (HMAC with the webhook secret) is checked first. The test makes a correctly signed message with the test
 * secret from MySqlTestDatabase (whsec_test_only_secret), exactly like Stripe would.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StripeWebhookTest {

	private static final String SECRET = "whsec_test_only_secret";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private String number;

	private String session;

	@BeforeEach
	void ravisPendingOrder() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		Cookie ravi = Logins.as(mvc, "ravi");
		long address = t.addressOf(2);
		t.addToCart(ravi, t.variantOf(1), 1);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null))
			.andReturn().getResponse().getContentAsString();
		number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		session = url.substring(url.lastIndexOf('/') + 1);
	}

	@AfterEach
	void clean() {
		t.clean();
	}

	private String event(String eventId, String type, String paymentStatus) {
		return """
				{"id": "%s", "object": "event", "type": "%s", "api_version": "2026-09-30.clover",
				 "data": {"object": {"id": "%s", "object": "checkout.session", "payment_status": "%s"}}}"""
			.formatted(eventId, type, session, paymentStatus);
	}

	private ResultActions send(String payload, String signature) throws Exception {
		return mvc.perform(post("/api/payments/stripe/webhook").contentType(MediaType.APPLICATION_JSON)
			.header("Stripe-Signature", signature).content(payload));
	}

	private static String sign(String payload) throws Exception {
		// Stripe's scheme: HMAC-SHA256 of "<timestamp>.<body>" with the webhook secret
		long now = System.currentTimeMillis() / 1000;
		return "t=" + now + ",v1=" + Webhook.Util.computeHmacSha256(SECRET, now + "." + payload);
	}

	private String orderStatus() {
		return jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, number);
	}

	@Test
	void badSignatureChangesNothing() throws Exception {
		send(event("evt_1", "checkout.session.completed", "paid"), "t=1,v1=forged").andExpect(status().isBadRequest());
		send(event("evt_1", "checkout.session.completed", "paid"), null).andExpect(status().isBadRequest());
		assertThat(orderStatus()).isEqualTo("PENDING_PAYMENT");
	}

	@Test
	void signedPaidEventPaysTheOrder() throws Exception {
		String payload = event("evt_paid_1", "checkout.session.completed", "paid");
		send(payload, sign(payload)).andExpect(status().isOk());
		assertThat(orderStatus()).isEqualTo("PAID");
	}

	/** Why can a webhook arrive twice, and what stops a double order? The event id (idempotency). */
	@Test
	void sameEventTwiceChangesTheOrderOnce() throws Exception {
		String payload = event("evt_twice", "checkout.session.completed", "paid");
		send(payload, sign(payload)).andExpect(status().isOk());
		send(payload, sign(payload)).andExpect(status().isOk()); // Stripe must still get 200, or it retries
		assertThat(orderStatus()).isEqualTo("PAID");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM processed_payment_events WHERE event_id = 'evt_twice'",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void unpaidOrOtherEventsAreIgnored() throws Exception {
		String unpaid = event("evt_unpaid", "checkout.session.completed", "unpaid");
		send(unpaid, sign(unpaid)).andExpect(status().isOk());
		String other = event("evt_other", "customer.created", "paid");
		send(other, sign(other)).andExpect(status().isOk());
		assertThat(orderStatus()).isEqualTo("PENDING_PAYMENT");
	}

}

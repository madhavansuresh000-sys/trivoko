package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.order.OrderService.PaidResult;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** After "Place order": paid (fake page), expired after 10 minutes, and paid too late. */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentLifecycleTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private OrderService orderService;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private long chennai;

	private long kovai;

	private long other;

	private String number;

	private String session;

	/** Ravi orders 2 + 1 items with WELCOME10; a third item stays in his cart (not ordered). */
	@BeforeEach
	void ravisPendingOrder() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		long address = t.addressOf(2);
		chennai = t.variantOf(1);
		kovai = t.variantOf(2);
		t.addToCart(ravi, chennai, 2);
		t.addToCart(ravi, kovai, 1);
		String json = t.place(ravi, address, "WELCOME10", t.previewTotal(ravi, address, "WELCOME10"))
			.andReturn().getResponse().getContentAsString();
		number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		session = url.substring(url.lastIndexOf('/') + 1);
		other = t.variantOf(3);
		t.addToCart(ravi, other, 1); // added after ordering: must stay in the cart after payment
	}

	@AfterEach
	void clean() {
		t.clean();
	}

	private String orderStatus() {
		return jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, number);
	}

	private void timeRunsOut() {
		jdbc.update("UPDATE orders SET hold_expires_at = NOW(6) - INTERVAL 1 MINUTE WHERE number = ?", number);
	}

	@Test
	void theTestPageShowsWhatToPay() throws Exception {
		mvc.perform(get("/api/payments/fake/" + session).cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orderNumber").value(number))
			.andExpect(jsonPath("$.status").value("CREATED"));
	}

	/** Phase 4 "done when": pay once for 2 sellers -> 2 packages PLACED. */
	@Test
	void payingPlacesEveryPackage() throws Exception {
		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(ravi).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.number").value(number))
			.andExpect(jsonPath("$.status").value("PAID"))
			.andExpect(jsonPath("$.packages[*].status", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("PLACED"))));

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM coupon_redemptions", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForList("SELECT variant_id FROM cart_items", Long.class)).containsExactly(other);
		assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE session_id = ?", String.class, session))
			.isEqualTo("PAID");
	}

	@Test
	void payingTwiceChangesNothing() throws Exception {
		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(ravi).with(csrf()));
		int stock = t.stock(chennai);
		assertThat(orderService.paymentReceived("session:" + session, session)).isEqualTo(PaidResult.ALREADY_DONE);
		assertThat(orderService.paymentReceived("evt_other_copy", session)).isEqualTo(PaidResult.ALREADY_DONE);
		assertThat(t.stock(chennai)).isEqualTo(stock);
	}

	@Test
	void someoneElseCannotPayOrSeeMyPaymentPage() throws Exception {
		Cookie kavya = Logins.as(mvc, "kavya");
		mvc.perform(get("/api/payments/fake/" + session).cookie(kavya)).andExpect(status().isNotFound());
		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(kavya).with(csrf()))
			.andExpect(status().isNotFound());
		assertThat(orderStatus()).isEqualTo("PENDING_PAYMENT");
	}

	@Test
	void unpaidOrderExpiresAndTheStockComesBack() {
		int held = t.stock(chennai);
		timeRunsOut();
		assertThat(orderService.expireOldHolds()).isEqualTo(1);
		assertThat(orderStatus()).isEqualTo("EXPIRED");
		assertThat(t.stock(chennai)).isEqualTo(held + 2);
		assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE session_id = ?", String.class, session))
			.isEqualTo("EXPIRED");
		assertThat(orderService.expireOldHolds()).isZero(); // nothing twice
	}

	@Test
	void latePaymentTakesTheStockAgainWhenItIsStillThere() {
		timeRunsOut();
		orderService.expireOldHolds();
		int free = t.stock(chennai);
		assertThat(orderService.paymentReceived("evt_late", session)).isEqualTo(PaidResult.PAID_LATE);
		assertThat(orderStatus()).isEqualTo("PAID");
		assertThat(t.stock(chennai)).isEqualTo(free - 2);
	}

	/** Review focus: paid after expiry and someone else bought the last units -> money back, order stays EXPIRED. */
	@Test
	void latePaymentWithoutStockIsRefunded() {
		timeRunsOut();
		orderService.expireOldHolds();
		jdbc.update("UPDATE product_variants SET stock = 0 WHERE id = ?", kovai);
		int chennaiFree = t.stock(chennai);
		assertThat(orderService.paymentReceived("evt_late", session)).isEqualTo(PaidResult.REFUNDED);
		assertThat(orderStatus()).isEqualTo("EXPIRED");
		assertThat(t.stock(chennai)).isEqualTo(chennaiFree); // the partial re-hold was undone too
		assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE session_id = ?", String.class, session))
			.isEqualTo("REFUNDED");
	}

	// ---------- My orders ----------

	@Test
	void myOrdersListAndDetail() throws Exception {
		mvc.perform(get("/api/orders").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].number").value(number))
			.andExpect(jsonPath("$.content[0].packageCount").value(2))
			.andExpect(jsonPath("$.content[0].itemCount").value(3));
		mvc.perform(get("/api/orders/" + number).cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.couponCode").value("WELCOME10"))
			.andExpect(jsonPath("$.packages.length()").value(2))
			.andExpect(jsonPath("$.shipTo.city").value("Chennai"));
	}

	@Test
	void someoneElsesOrderIsNotFound() throws Exception {
		mvc.perform(get("/api/orders/" + number).cookie(Logins.as(mvc, "kavya"))).andExpect(status().isNotFound());
	}

}

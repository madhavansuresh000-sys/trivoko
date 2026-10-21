package com.trivoko.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Paying an order sends one email to the customer and one to EACH seller of the order - only after the
 * payment is committed, and again later if the mail server was down. (The mail server is replaced by a mock.)
 */
@SpringBootTest(properties = "management.health.mail.enabled=false") // the mail health check needs a real sender
@AutoConfigureMockMvc
class OrderEmailsTest {

	@MockitoBean
	private JavaMailSender mail;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EmailSender emails;

	private Cookie ravi;

	private String session;

	private String number;

	/** variant id -> stock before the test (put back afterwards: these tests do not roll back) */
	private final java.util.Map<Long, Integer> stockBefore = new java.util.HashMap<>();

	@BeforeEach
	void ravisPendingOrderFromTwoSellers() throws Exception {
		ravi = Logins.as(mvc, "ravi");
		jdbc.update("""
				INSERT INTO addresses (user_id, name, phone, line1, city, state, pincode, is_default)
				VALUES (2, 'Ravi Kumar', '9000000002', '12 Anna Salai', 'Chennai', 'Tamil Nadu', '600002', TRUE)""");
		long address = jdbc.queryForObject("SELECT MAX(id) FROM addresses", Long.class);
		for (long seller : new long[] { 1, 2 }) {
			long variant = jdbc.queryForObject("""
					SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
					WHERE p.seller_id = ? AND p.status = 'ACTIVE' AND v.stock >= 20 ORDER BY v.id LIMIT 1""", Long.class, seller);
			stockBefore.put(variant, jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variant));
			mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/cart/items/" + variant)
				.cookie(ravi).with(csrf()).contentType("application/json").content("{\"quantity\":1}"));
		}
		String preview = mvc.perform(post("/api/checkout/preview").cookie(ravi).with(csrf())
			.contentType("application/json").content("{\"addressId\":" + address + "}")).andReturn().getResponse().getContentAsString();
		String placed = mvc.perform(post("/api/orders").cookie(ravi).with(csrf()).contentType("application/json")
			.content("{\"addressId\":" + address + ",\"expectedTotal\":" + JsonPath.read(preview, "$.grandTotal") + "}"))
			.andReturn().getResponse().getContentAsString();
		number = JsonPath.read(placed, "$.number");
		String url = JsonPath.read(placed, "$.redirectUrl");
		session = url.substring(url.lastIndexOf('/') + 1);
	}

	@AfterEach
	void clean() {
		for (String table : List.of("notifications", "payments", "order_items", "packages", "orders", "cart_items", "carts",
				"addresses", "processed_payment_events")) {
			jdbc.update("DELETE FROM " + table);
		}
		stockBefore.forEach((id, stock) -> jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stock, id));
	}

	private void pay() throws Exception {
		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(ravi).with(csrf()));
	}

	@Test
	void customerAndEachSellerGetAnEmail() throws Exception {
		pay();
		ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mail, org.mockito.Mockito.timeout(10_000).times(3)).send(sent.capture()); // sent in the background
		assertThat(sent.getAllValues()).extracting(m -> m.getTo()[0])
			.containsExactlyInAnyOrder("ravi@trivoko.test", "chennai.mobiles@trivoko.test", "kovai.sports@trivoko.test");
		assertThat(sent.getAllValues()).anySatisfy(m -> assertThat(m.getSubject()).isEqualTo("Order " + number + " is placed"));
		assertThat(sent.getAllValues()).anySatisfy(m -> assertThat(m.getSubject()).startsWith("New package to pack: " + number));
	}

	@Test
	void theBellShowsTheOrderToTheCustomer() throws Exception {
		pay();
		mvc.perform(get("/api/notifications").cookie(ravi))
			.andExpect(jsonPath("$.unread").value(1))
			.andExpect(jsonPath("$.items[0].kind").value("ORDER_PLACED"))
			.andExpect(jsonPath("$.items[0].link").value("/orders/" + number));
	}

	@Test
	void noEmailBeforeThePaymentIsCommitted() throws Exception {
		verify(mail, times(0)).send(any(SimpleMailMessage.class)); // placing the order sends nothing
	}

	/** Found in the browser: a slow mail server made "Pay" time out although the order WAS paid. */
	@Test
	void slowMailServerDoesNotSlowDownPaying() throws Exception {
		org.mockito.Mockito.doAnswer(call -> {
			Thread.sleep(3000); // a mail server that takes 3 s per email
			return null;
		}).when(mail).send(any(SimpleMailMessage.class));

		long start = System.nanoTime();
		pay();
		long millis = (System.nanoTime() - start) / 1_000_000;

		assertThat(millis).as("payment answered in %d ms", millis).isLessThan(2000);
		assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, number)).isEqualTo("PAID");
		// the emails still go out, a little later, in the background
		verify(mail, org.mockito.Mockito.timeout(15_000).times(3)).send(any(SimpleMailMessage.class));
	}

	@Test
	void mailServerDownIsRetriedLater() throws Exception {
		doThrow(new MailSendException("Mailpit is down")).when(mail).send(any(SimpleMailMessage.class));
		pay();
		verify(mail, org.mockito.Mockito.timeout(10_000).times(3)).send(any(SimpleMailMessage.class)); // 3 failed tries
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE email_status = 'PENDING'", Integer.class)).isEqualTo(3);

		doNothing().when(mail).send(any(SimpleMailMessage.class));
		assertThat(emails.sendPending(LocalDateTime.now().plusMinutes(1))).isEqualTo(3);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE email_status = 'SENT'", Integer.class)).isEqualTo(3);
	}

}

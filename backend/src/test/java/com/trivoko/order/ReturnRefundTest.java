package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 8 steps 7-8 + 10 - the money. Ravi pays ONE payment (coupon WELCOME10) for two Kovai Sports items and one
 * Chennai Mobiles item. Kovai's package is delivered; Ravi returns a Kovai item; "picked up" refunds exactly
 * line total - its coupon share, the item goes back on the shelf, Kovai's ledger gets -90% - and a double click
 * (or ten at once) still refunds once.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReturnRefundTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private Cookie kovai;

	private long variantA;

	private long variantB;

	private int stockBBefore;

	private long orderId;

	private long kovaiPackage;

	@BeforeEach
	void raviPaysOnceForThreeItemsAndKovaiDelivers() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		kovai = Logins.as(mvc, "kovai.sports");
		long address = t.addressOf(2);
		variantA = t.variantOf(2);
		variantB = jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.seller_id = 2 AND p.status = 'ACTIVE' AND v.stock >= 20 AND v.id <> ? ORDER BY v.id LIMIT 1""",
				Long.class, variantA);
		stockBBefore = t.stock(variantB);
		t.addToCart(ravi, variantA, 1);
		t.addToCart(ravi, variantB, 1);
		t.addToCart(ravi, t.variantOf(1), 1);
		String json = t.place(ravi, address, "WELCOME10", t.previewTotal(ravi, address, "WELCOME10")).andReturn()
			.getResponse().getContentAsString();
		String number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi)
			.with(csrf()));
		orderId = jdbc.queryForObject("SELECT id FROM orders WHERE number = ?", Long.class, number);
		kovaiPackage = jdbc.queryForObject("SELECT id FROM packages WHERE order_id = ? AND seller_id = 2", Long.class,
				orderId);
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/pack").cookie(kovai).with(csrf()));
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/ship").cookie(kovai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNumber\":\"AWB445566\"}"));
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/deliver").cookie(kovai).with(csrf()))
			.andExpect(status().isOk());
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications");
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stockBBefore, variantB);
		t.clean();
	}

	private long lineOf(long variantId) {
		return jdbc.queryForObject("SELECT id FROM order_items WHERE package_id = ? AND variant_id = ?", Long.class,
				kovaiPackage, variantId);
	}

	/** Ravi asks, Kovai approves; returns the return id. */
	private long approvedReturn(long line) throws Exception {
		String json = mvc.perform(post("/api/me/returns").cookie(ravi).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"orderItemId\":" + line + ",\"reason\":\"DAMAGED\",\"note\":\"Arrived cracked\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(json, "$.id")).longValue();
		mvc.perform(post("/api/seller/returns/" + id + "/approve").cookie(kovai).with(csrf()))
			.andExpect(status().isOk());
		return id;
	}

	private ResultActions pickedUp(long returnId) throws Exception {
		return mvc.perform(post("/api/seller/returns/" + returnId + "/picked-up").cookie(kovai).with(csrf()));
	}

	private BigDecimal refunded() {
		return jdbc.queryForObject("SELECT refunded_amount FROM payments WHERE order_id = ? AND status <> 'EXPIRED'"
				+ " ORDER BY id DESC LIMIT 1", BigDecimal.class, orderId);
	}

	@Test
	void pickedUpRefundsExactlyTheLineMinusItsCouponShare() throws Exception {
		long line = lineOf(variantA);
		BigDecimal lineTotal = jdbc.queryForObject("SELECT line_total FROM order_items WHERE id = ?", BigDecimal.class, line);
		BigDecimal share = jdbc.queryForObject("SELECT discount_share FROM order_items WHERE id = ?", BigDecimal.class, line);
		assertThat(share).isPositive(); // the coupon really was split over the lines
		BigDecimal expected = lineTotal.subtract(share);
		int stockBeforeRefund = t.stock(variantA);
		long id = approvedReturn(line);

		pickedUp(id)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REFUNDED"))
			.andExpect(jsonPath("$.events.length()").value(4)); // REQUESTED, APPROVED, PICKED_UP, REFUNDED

		assertThat(refunded()).isEqualByComparingTo(expected); // only this line - not the whole order
		assertThat(t.stock(variantA)).isEqualTo(stockBeforeRefund + 1); // back on the shelf
		BigDecimal minus = jdbc.queryForObject(
				"SELECT amount FROM seller_ledger WHERE kind = 'REFUND' AND return_id = ?", BigDecimal.class, id);
		assertThat(minus).isEqualByComparingTo(expected.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP)
			.negate());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = 2 AND kind = 'RETURN_REFUNDED'",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void aSecondClickRefundsNothing() throws Exception {
		long id = approvedReturn(lineOf(variantA));
		pickedUp(id).andExpect(status().isOk());
		BigDecimal once = refunded();

		pickedUp(id).andExpect(status().isConflict());
		mvc.perform(post("/api/admin/returns/" + id + "/retry-refund").cookie(Logins.as(mvc, "admin")).with(csrf()))
			.andExpect(status().isConflict()); // only a FAILED refund can be retried
		assertThat(refunded()).isEqualByComparingTo(once);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM seller_ledger WHERE kind = 'REFUND'", Integer.class))
			.isEqualTo(1);
	}

	@Test
	void tenClicksAtTheSameMomentRefundOnce() throws Exception {
		long line = lineOf(variantA);
		BigDecimal expected = jdbc.queryForObject("SELECT line_total - discount_share FROM order_items WHERE id = ?",
				BigDecimal.class, line);
		long id = approvedReturn(line);

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(10);
		List<Future<Integer>> clicks = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			clicks.add(pool.submit(() -> {
				start.await();
				return pickedUp(id).andReturn().getResponse().getStatus();
			}));
		}
		start.countDown();
		int ok = 0;
		for (Future<Integer> c : clicks) {
			if (c.get(60, TimeUnit.SECONDS) == 200) {
				ok++;
			}
		}
		pool.shutdown();

		assertThat(ok).isEqualTo(1);
		assertThat(refunded()).isEqualByComparingTo(expected);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM seller_ledger WHERE kind = 'REFUND'", Integer.class))
			.isEqualTo(1);
	}

	@Test
	void twoItemsOfOnePackageGiveTwoLedgerLines() throws Exception {
		pickedUp(approvedReturn(lineOf(variantA))).andExpect(status().isOk());
		pickedUp(approvedReturn(lineOf(variantB))).andExpect(status().isOk());

		assertThat(jdbc.queryForObject(
				"SELECT COUNT(*) FROM seller_ledger WHERE package_id = ? AND kind = 'REFUND'", Integer.class, kovaiPackage))
			.isEqualTo(2); // the V10 key: one REFUND line per return, not per package
		assertThat(jdbc.queryForObject(
				"SELECT COUNT(*) FROM seller_ledger WHERE package_id = ? AND kind = 'SALE'", Integer.class, kovaiPackage))
			.isEqualTo(1);
	}

}

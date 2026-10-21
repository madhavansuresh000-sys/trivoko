package com.trivoko.order;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * Helpers for the checkout tests. These tests do NOT run in one rolled-back transaction (they must see what
 * really reached the database, e.g. stock after a rollback), so clean() puts everything back afterwards.
 */
final class CheckoutTestSupport {

	private final MockMvc mvc;

	private final JdbcTemplate jdbc;

	private final Map<Long, Integer> stockBefore = new HashMap<>();

	CheckoutTestSupport(MockMvc mvc, JdbcTemplate jdbc) {
		this.mvc = mvc;
		this.jdbc = jdbc;
	}

	/** A buyable variant of this seller with at least 20 in stock (remembers its stock for clean()). */
	long variantOf(long sellerId) {
		long id = jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.seller_id = ? AND p.status = 'ACTIVE' AND v.stock >= 20 ORDER BY v.id LIMIT 1""",
				Long.class, sellerId);
		stockBefore.putIfAbsent(id, stock(id));
		return id;
	}

	int stock(long variantId) {
		return jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variantId);
	}

	BigDecimal price(long variantId) {
		return jdbc.queryForObject("SELECT price FROM product_variants WHERE id = ?", BigDecimal.class, variantId);
	}

	/** A delivery address for this user (ravi = 2, kavya = 3). */
	long addressOf(long userId) {
		jdbc.update("""
				INSERT INTO addresses (user_id, name, phone, line1, city, state, pincode, is_default)
				VALUES (?, 'Test Person', '9000000099', '12 Anna Salai', 'Chennai', 'Tamil Nadu', '600002', TRUE)""", userId);
		return jdbc.queryForObject("SELECT MAX(id) FROM addresses WHERE user_id = ?", Long.class, userId);
	}

	void addToCart(Cookie who, long variantId, int quantity) throws Exception {
		mvc.perform(put("/api/cart/items/" + variantId).cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}"));
	}

	ResultActions preview(Cookie who, Long addressId, String coupon) throws Exception {
		String body = "{" + (addressId == null ? "" : "\"addressId\":" + addressId)
				+ (coupon == null ? "" : (addressId == null ? "" : ",") + "\"couponCode\":\"" + coupon + "\"") + "}";
		return mvc.perform(post("/api/checkout/preview").cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	/** The grand total the preview shows right now (what a customer would see before pressing Pay). */
	String previewTotal(Cookie who, long addressId, String coupon) throws Exception {
		String json = preview(who, addressId, coupon).andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.grandTotal").toString();
	}

	ResultActions place(Cookie who, long addressId, String coupon, String expectedTotal) throws Exception {
		String body = "{\"addressId\":" + addressId + (coupon == null ? "" : ",\"couponCode\":\"" + coupon + "\"")
				+ ",\"expectedTotal\":" + expectedTotal + "}";
		var request = post("/api/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
		return mvc.perform(who == null ? request : request.cookie(who)); // null = not logged in
	}

	int orderCount() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
	}

	/** Puts the database back as the seed left it. */
	void clean() {
		jdbc.update("DELETE FROM coupon_redemptions");
		jdbc.update("DELETE FROM payments");
		jdbc.update("DELETE FROM order_items");
		jdbc.update("DELETE FROM packages");
		jdbc.update("DELETE FROM orders");
		jdbc.update("DELETE FROM cart_items");
		jdbc.update("DELETE FROM carts");
		jdbc.update("DELETE FROM addresses");
		jdbc.update("DELETE FROM processed_payment_events");
		stockBefore.forEach((id, stock) -> jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stock, id));
		stockBefore.clear();
	}

}

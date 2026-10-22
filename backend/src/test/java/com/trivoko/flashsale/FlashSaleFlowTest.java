package com.trivoko.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.order.OrderService;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 7 end to end through the real URLs: schedule -> offer -> claim -> checkout at the sale price -> pay,
 * plus the give-back and the end of the sale. (Not one rolled-back transaction: it must see what really
 * reached the database, so clean() puts everything back.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlashSaleFlowTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private FlashSaleService saleService;

	@Autowired
	private FlashClaimService claimService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private ClaimRateLimiter limiter;

	private Cookie admin;

	private Cookie chennai;

	private Cookie ravi;

	private Cookie kavya;

	private long variant;

	private int stockBefore;

	private BigDecimal normalPrice;

	@BeforeEach
	void people() throws Exception {
		admin = Logins.as(mvc, "admin");
		chennai = Logins.as(mvc, "chennai.mobiles");
		ravi = Logins.as(mvc, "ravi");
		kavya = Logins.as(mvc, "kavya");
		variant = jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.seller_id = 1 AND p.status = 'ACTIVE' AND v.stock >= 20 ORDER BY v.id LIMIT 1""", Long.class);
		stockBefore = stock();
		normalPrice = jdbc.queryForObject("SELECT price FROM product_variants WHERE id = ?", BigDecimal.class, variant);
		jdbc.update("DELETE FROM flash_sale_claims");
		limiter.reset();
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications"); // paying an order sends bell + email rows
		jdbc.update("DELETE FROM payments");
		jdbc.update("DELETE FROM order_items");
		jdbc.update("DELETE FROM package_events");
		jdbc.update("DELETE FROM seller_ledger");
		jdbc.update("DELETE FROM packages");
		jdbc.update("DELETE FROM orders");
		jdbc.update("DELETE FROM cart_items");
		jdbc.update("DELETE FROM carts");
		jdbc.update("DELETE FROM addresses");
		jdbc.update("DELETE FROM processed_payment_events");
		jdbc.update("DELETE FROM flash_sale_claims");
		jdbc.update("DELETE FROM flash_sale_items");
		jdbc.update("DELETE FROM flash_sales");
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stockBefore, variant);
	}

	private int stock() {
		return jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variant);
	}

	/** A sale made through the admin URL; start/end in minutes from now (negative = already started). */
	private long saleApi(int startMinutes, int endMinutes) throws Exception {
		LocalDateTime now = LocalDateTime.now();
		String body = "{\"title\":\"Midnight Deals\",\"startsAt\":\"" + now.plusMinutes(startMinutes) + "\",\"endsAt\":\""
				+ now.plusMinutes(endMinutes) + "\"}";
		String json = mvc.perform(post("/api/admin/flash-sales").cookie(admin).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private String offerBody(String price, int quantity) {
		return "{\"variantId\":" + variant + ",\"salePrice\":" + price + ",\"quantity\":" + quantity + "}";
	}

	/** Offer through the seller URL; returns the deal id. */
	private long offerApi(long saleId, String price, int quantity) throws Exception {
		String json = mvc.perform(post("/api/seller/flash-sales/" + saleId + "/offers").cookie(chennai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(offerBody(price, quantity)))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.itemId")).longValue();
	}

	/** Moves the sale window so that it is running now (the offer needs the sale to be in the future). */
	private void startNow(long saleId) {
		jdbc.update("UPDATE flash_sales SET starts_at = ?, ends_at = ? WHERE id = ?", LocalDateTime.now().minusMinutes(1),
				LocalDateTime.now().plusMinutes(30), saleId);
	}

	private int sold(long itemId) {
		return jdbc.queryForObject("SELECT sold FROM flash_sale_items WHERE id = ?", Integer.class, itemId);
	}

	// ---------- setting up ----------

	@Test
	void offeringTakesTheUnitsOutOfNormalStock() throws Exception {
		long sale = saleApi(10, 60);
		offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 7);
		assertThat(stock()).isEqualTo(stockBefore - 7);
	}

	@Test
	void withdrawingBeforeTheStartGivesTheUnitsBack() throws Exception {
		long sale = saleApi(10, 60);
		long item = offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 7);
		mvc.perform(delete("/api/seller/flash-sales/offers/" + item).cookie(chennai).with(csrf()))
			.andExpect(status().isNoContent());
		assertThat(stock()).isEqualTo(stockBefore);
	}

	@Test
	void theSalePriceMustBeLowerAndTheVariantMustBeMine() throws Exception {
		long sale = saleApi(10, 60);
		mvc.perform(post("/api/seller/flash-sales/" + sale + "/offers").cookie(chennai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(offerBody(normalPrice.toPlainString(), 3)))
			.andExpect(status().isConflict());
		Cookie kovai = Logins.as(mvc, "kovai.sports"); // not the owner of this variant -> 404, nothing leaks
		mvc.perform(post("/api/seller/flash-sales/" + sale + "/offers").cookie(kovai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(offerBody("1.00", 3)))
			.andExpect(status().isNotFound());
		assertThat(stock()).isEqualTo(stockBefore);
	}

	@Test
	void aCustomerCannotScheduleASaleOrOffer() throws Exception {
		mvc.perform(post("/api/admin/flash-sales").cookie(ravi).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"x\",\"startsAt\":\"2030-01-01T10:00:00\",\"endsAt\":\"2030-01-01T11:00:00\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/seller/flash-sales/1/offers").cookie(ravi).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(offerBody("1.00", 1))).andExpect(status().isForbidden());
	}

	@Test
	void aVariantCannotBeInTwoOverlappingSales() throws Exception {
		long first = saleApi(10, 60);
		long second = saleApi(30, 90);
		offerApi(first, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 2);
		mvc.perform(post("/api/seller/flash-sales/" + second + "/offers").cookie(chennai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(offerBody(normalPrice.subtract(BigDecimal.TEN).toPlainString(), 2)))
			.andExpect(status().isConflict());
	}

	@Test
	void thePublicPageShowsTheSaleWithoutLogin() throws Exception {
		long sale = saleApi(-1, 60);
		mvc.perform(get("/api/flash-sales/current")).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(sale)).andExpect(jsonPath("$.serverNow").exists());
	}

	// ---------- claiming ----------

	@Test
	void claimingNeedsALoginAndASaleThatIsLive() throws Exception {
		long sale = saleApi(10, 60);
		long item = offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 2);
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").with(csrf())).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(ravi).with(csrf()))
			.andExpect(status().isConflict()).andExpect(content().string(containsString("not started")));
		assertThat(sold(item)).isZero();
	}

	@Test
	void oneUnitPerPersonAndSoldOutIsAnswered() throws Exception {
		long sale = saleApi(10, 60);
		long item = offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 1);
		startNow(sale);
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(ravi).with(csrf()))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("HELD"));
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(ravi).with(csrf()))
			.andExpect(status().isConflict()).andExpect(content().string(containsString("already claimed")));
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(kavya).with(csrf()))
			.andExpect(status().isConflict()).andExpect(content().string(containsString("Sold out")));
		assertThat(sold(item)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flash_sale_claims", Integer.class)).isEqualTo(1);
	}

	@Test
	void tooManyTriesAreSlowedDownWithRetryAfter() throws Exception {
		long sale = saleApi(10, 60);
		long item = offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 1);
		startNow(sale);
		int tooMany = 0;
		for (int i = 0; i < 8; i++) { // limit is 5 tries per 10 seconds per person
			var result = mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(kavya).with(csrf())).andReturn();
			if (result.getResponse().getStatus() == 429) {
				tooMany++;
				assertThat(result.getResponse().getHeader("Retry-After")).isNotNull();
			}
		}
		assertThat(tooMany).isGreaterThanOrEqualTo(3);
	}

	// ---------- checkout and payment ----------

	/** Ravi claims the deal, checks out and gets the payment session id. */
	private String claimAndOrder(long item, Cookie who, long userId, String expectedTotal) throws Exception {
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(who).with(csrf())).andExpect(status().isCreated());
		jdbc.update("""
				INSERT INTO addresses (user_id, name, phone, line1, city, state, pincode, is_default)
				VALUES (?, 'Test Person', '9000000099', '12 Anna Salai', 'Chennai', 'Tamil Nadu', '600002', TRUE)""", userId);
		long address = jdbc.queryForObject("SELECT MAX(id) FROM addresses WHERE user_id = ?", Long.class, userId);
		String preview = mvc.perform(post("/api/checkout/preview").cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"addressId\":" + address + "}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(new BigDecimal(JsonPath.read(preview, "$.packages[0].items[0].unitPrice").toString())).isEqualByComparingTo(expectedTotal);
		String total = JsonPath.read(preview, "$.grandTotal").toString();
		String json = mvc.perform(post("/api/orders").cookie(who).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"addressId\":" + address + ",\"expectedTotal\":" + total + "}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String url = JsonPath.read(json, "$.redirectUrl");
		return url.substring(url.lastIndexOf('/') + 1);
	}

	@Test
	void aClaimIsBoughtAtTheSalePriceAndNormalStockIsNotTouched() throws Exception {
		BigDecimal salePrice = normalPrice.subtract(BigDecimal.TEN);
		long sale = saleApi(10, 60);
		long item = offerApi(sale, salePrice.toPlainString(), 3);
		startNow(sale);
		int stockAfterOffer = stock();

		String session = claimAndOrder(item, ravi, 2, salePrice.toPlainString());
		assertThat(stock()).isEqualTo(stockAfterOffer); // the unit came from the sale, not from the shelf
		assertThat(jdbc.queryForObject("SELECT status FROM flash_sale_claims", String.class)).isEqualTo("ORDERED");
		assertThat(jdbc.queryForObject("SELECT flash_item_id FROM order_items", Long.class)).isEqualTo(item);

		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(ravi).with(csrf())).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT status FROM flash_sale_claims", String.class)).isEqualTo("PAID");
		assertThat(sold(item)).isEqualTo(1);
		assertThat(stock()).isEqualTo(stockAfterOffer);
	}

	@Test
	void anUnpaidOrderGivesTheUnitBackAfterTheHoldRunsOut() throws Exception {
		BigDecimal salePrice = normalPrice.subtract(BigDecimal.TEN);
		long sale = saleApi(10, 60);
		long item = offerApi(sale, salePrice.toPlainString(), 1);
		startNow(sale);
		claimAndOrder(item, ravi, 2, salePrice.toPlainString());
		assertThat(sold(item)).isEqualTo(1);

		// the 10 minutes pass: the claim and the order are both old
		jdbc.update("UPDATE flash_sale_claims SET expires_at = ?", LocalDateTime.now().minusMinutes(1));
		jdbc.update("UPDATE orders SET hold_expires_at = ?", LocalDateTime.now().minusMinutes(1));
		orderService.expireOldHolds();

		assertThat(sold(item)).isZero(); // THE GIVE-BACK
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flash_sale_claims", Integer.class)).isZero();
		// and somebody else can now get the unit
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(kavya).with(csrf())).andExpect(status().isCreated());
	}

	@Test
	void aClaimNobodyCheckedOutIsGivenBackByTheJob() throws Exception {
		long sale = saleApi(10, 60);
		long item = offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 2);
		startNow(sale);
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(ravi).with(csrf())).andExpect(status().isCreated());
		assertThat(claimService.expireHeld()).isZero(); // still inside its 10 minutes
		jdbc.update("UPDATE flash_sale_claims SET expires_at = ?", LocalDateTime.now().minusSeconds(5));
		assertThat(claimService.expireHeld()).isEqualTo(1);
		assertThat(sold(item)).isZero();
	}

	@Test
	void aChangedCheckoutKeepsTheClaimForAnotherTry() throws Exception {
		BigDecimal salePrice = normalPrice.subtract(BigDecimal.TEN);
		long sale = saleApi(10, 60);
		long item = offerApi(sale, salePrice.toPlainString(), 1);
		startNow(sale);
		claimAndOrder(item, ravi, 2, salePrice.toPlainString());
		// the order is replaced (expired) while the claim is still inside its own 10 minutes -> back to HELD
		jdbc.update("UPDATE orders SET hold_expires_at = ?", LocalDateTime.now().minusMinutes(1));
		orderService.expireOldHolds();
		assertThat(jdbc.queryForObject("SELECT status FROM flash_sale_claims", String.class)).isEqualTo("HELD");
		assertThat(sold(item)).isEqualTo(1);
	}

	// ---------- the end of the sale ----------

	@Test
	void whenTheSaleEndsUnsoldUnitsGoBackToNormalStock() throws Exception {
		long sale = saleApi(10, 60);
		long item = offerApi(sale, normalPrice.subtract(BigDecimal.TEN).toPlainString(), 5);
		startNow(sale);
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(ravi).with(csrf())).andExpect(status().isCreated());
		assertThat(stock()).isEqualTo(stockBefore - 5);

		jdbc.update("UPDATE flash_sales SET starts_at = ?, ends_at = ?", LocalDateTime.now().minusHours(2),
				LocalDateTime.now().minusHours(1));
		assertThat(saleService.settleEnded()).isEqualTo(1);
		assertThat(stock()).isEqualTo(stockBefore - 1); // 4 unsold are back, the 1 claimed stays out
		assertThat(saleService.settleEnded()).isZero(); // only once

		// a claim given back AFTER the settle goes straight to normal stock
		jdbc.update("UPDATE flash_sale_claims SET expires_at = ?", LocalDateTime.now().minusMinutes(1));
		claimService.expireHeld();
		assertThat(stock()).isEqualTo(stockBefore);
		mvc.perform(post("/api/flash-sales/items/" + item + "/claim").cookie(kavya).with(csrf()))
			.andExpect(status().isConflict()).andExpect(header().doesNotExist("Retry-After"));
	}

}

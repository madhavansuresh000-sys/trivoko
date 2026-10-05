package com.trivoko.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.coupon.CouponService.CouponQuote;
import com.trivoko.support.Logins;

/** The coupon rules (seed: WELCOME10 = 10% up to Rs 500 from Rs 499; FLAT100 = Rs 100 off from Rs 999). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponServiceTest {

	private static final long RAVI = 2;

	private static final long KAVYA = 3;

	@Autowired
	private CouponService coupons;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private MockMvc mvc;

	private CouponQuote quote(String code, long user, String itemsTotal) {
		return coupons.quote(code, user, new BigDecimal(itemsTotal), false);
	}

	@Test
	void percentCouponTakesTenPercent() {
		CouponQuote q = quote("WELCOME10", RAVI, "3497");
		assertThat(q.error()).isNull();
		assertThat(q.code()).isEqualTo("WELCOME10");
		assertThat(q.discount()).isEqualByComparingTo("349.70");
	}

	@Test
	void percentCouponIsCappedAtMaxDiscount() {
		assertThat(quote("WELCOME10", RAVI, "6000").discount()).isEqualByComparingTo("500.00");
	}

	@Test
	void codeIsNotCaseSensitive() {
		assertThat(quote("  welcome10 ", RAVI, "1000").discount()).isEqualByComparingTo("100.00");
	}

	@Test
	void belowMinimumSaysHowMuchMoreIsNeeded() {
		CouponQuote q = quote("WELCOME10", RAVI, "400");
		assertThat(q.error()).isEqualTo("Add ₹99 more to use WELCOME10");
		assertThat(q.discount()).isEqualByComparingTo("0");
	}

	@Test
	void flatCouponTakesItsValue() {
		assertThat(quote("FLAT100", RAVI, "999").discount()).isEqualByComparingTo("100.00");
	}

	@Test
	void unknownCode() {
		assertThat(quote("FREEFOOD", RAVI, "999").error()).isEqualTo("This coupon does not exist");
	}

	@Test
	void expiredCode() {
		jdbc.update("UPDATE coupons SET valid_until = NOW(6) - INTERVAL 1 HOUR WHERE code = 'FLAT100'");
		assertThat(quote("FLAT100", RAVI, "2000").error()).isEqualTo("This coupon has expired");
	}

	@Test
	void switchedOffCode() {
		jdbc.update("UPDATE coupons SET active = FALSE WHERE code = 'FLAT100'");
		assertThat(quote("FLAT100", RAVI, "2000").error()).isEqualTo("This coupon is no longer available");
	}

	@Test
	void alreadyInAnUnpaidOrder() {
		assertThat(coupons.quote("WELCOME10", RAVI, new BigDecimal("1000"), true).error())
			.isEqualTo("WELCOME10 is already used in your unpaid order");
	}

	/** Review focus: one use per customer - another customer can still use it. */
	@Test
	void onePerCustomer() {
		long orderId = anOrderOf(RAVI);
		coupons.redeem("WELCOME10", RAVI, orderId);
		assertThat(quote("WELCOME10", RAVI, "1000").error()).isEqualTo("You have already used WELCOME10");
		assertThat(quote("WELCOME10", KAVYA, "1000").error()).isNull();
	}

	/** An order row for the foreign key of coupon_redemptions. */
	private long anOrderOf(long userId) {
		jdbc.update("""
				INSERT INTO orders (number, user_id, status, items_total, discount_total, shipping_total, grand_total,
				  ship_name, ship_phone, ship_line1, ship_city, ship_state, ship_pincode, hold_expires_at)
				VALUES ('TV-999001', ?, 'PAID', 1000, 100, 0, 900, 'Ravi', '9000000002', '1 Street', 'Chennai',
				  'Tamil Nadu', '600001', NOW(6))""", userId);
		return jdbc.queryForObject("SELECT id FROM orders WHERE number = 'TV-999001'", Long.class);
	}

	// ---------- URLs ----------

	@Test
	void customerCanCheckACode() throws Exception {
		mvc.perform(get("/api/coupons/welcome10").cookie(Logins.as(mvc, "ravi")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("WELCOME10"))
			.andExpect(jsonPath("$.description").exists());
		mvc.perform(get("/api/coupons/NOPE").cookie(Logins.as(mvc, "ravi")))
			.andExpect(status().isNotFound());
	}

	@Test
	void adminCreatesCouponsInUpperCase() throws Exception {
		mvc.perform(post("/api/admin/coupons").cookie(Logins.as(mvc, "admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"code":"diwali25","description":"Rs 250 off on Diwali","type":"FLAT","value":250,
						 "minOrder":1500,"validUntil":"2030-01-01T00:00:00"}"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("DIWALI25"));
		assertThat(quote("DIWALI25", RAVI, "1500").discount()).isEqualByComparingTo("250.00");
	}

	@Test
	void sameCodeTwiceIsRefused() throws Exception {
		mvc.perform(post("/api/admin/coupons").cookie(Logins.as(mvc, "admin")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"code":"welcome10","description":"again","type":"FLAT","value":10,"minOrder":0,
						 "validUntil":"2030-01-01T00:00:00"}"""))
			.andExpect(status().isConflict());
	}

	@Test
	void onlyTheAdminCreatesCoupons() throws Exception {
		mvc.perform(post("/api/admin/coupons").cookie(Logins.as(mvc, "ravi")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isForbidden());
	}

}

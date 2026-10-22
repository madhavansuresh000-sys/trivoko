package com.trivoko.order;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 6: the admin finds orders (by number or customer email) and reads the reports. Ravi pays for 2 units of
 * one Kovai Sports item, so every report has exactly one known sale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminOrdersAndReportsTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private CacheManager cacheManager;

	private CheckoutTestSupport t;

	private Cookie admin;

	private String number;

	private BigDecimal packageTotal;

	private BigDecimal lineTotal;

	@BeforeEach
	void ravisPaidOrder() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		Cookie ravi = Logins.as(mvc, "ravi");
		admin = Logins.as(mvc, "admin");
		long address = t.addressOf(2);
		t.addToCart(ravi, t.variantOf(2), 2);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse().getContentAsString();
		number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi).with(csrf()));
		packageTotal = jdbc.queryForObject("SELECT total FROM packages WHERE seller_id = 2", BigDecimal.class);
		lineTotal = jdbc.queryForObject("SELECT line_total FROM order_items", BigDecimal.class);
	}

	@AfterEach
	void clean() {
		var cache = cacheManager.getCache("adminReports");
		if (cache != null) {
			cache.clear();
		}
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	@Test
	void findAnOrderByCustomerEmail() throws Exception {
		mvc.perform(get("/api/admin/orders").param("q", "Ravi@TriVoKo.test").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].number").value(number))
			.andExpect(jsonPath("$.content[0].customerEmail").value("ravi@trivoko.test"))
			.andExpect(jsonPath("$.content[0].status").value("PAID"));
	}

	@Test
	void findAnOrderByPartOfItsNumber() throws Exception {
		mvc.perform(get("/api/admin/orders").param("q", number.substring(0, 6).toLowerCase()).cookie(admin))
			.andExpect(jsonPath("$.content[0].number").value(number));
		mvc.perform(get("/api/admin/orders").param("q", "nobody@example.com").cookie(admin))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void orderDetailShowsTheCustomer() throws Exception {
		mvc.perform(get("/api/admin/orders/" + number).cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customerEmail").value("ravi@trivoko.test"))
			.andExpect(jsonPath("$.order.packages[0].sellerName").value("Kovai Sports"));
	}

	@Test
	void salesReportHasOnePointPerDay() throws Exception {
		mvc.perform(get("/api/admin/reports/sales").param("days", "7").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(7))
			.andExpect(jsonPath("$[6].total").value(packageTotal.doubleValue()))
			.andExpect(jsonPath("$[6].packages").value(1))
			.andExpect(jsonPath("$[0].total").value(0));
	}

	@Test
	void topSellersAndProducts() throws Exception {
		BigDecimal fee = packageTotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
		mvc.perform(get("/api/admin/reports/top-sellers").param("days", "30").cookie(admin))
			.andExpect(jsonPath("$[0].shopName").value("Kovai Sports"))
			.andExpect(jsonPath("$[0].packages").value(1))
			.andExpect(jsonPath("$[0].revenue").value(packageTotal.doubleValue()))
			.andExpect(jsonPath("$[0].fee").value(fee.doubleValue()));
		mvc.perform(get("/api/admin/reports/top-products").param("days", "30").cookie(admin))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].units").value(2))
			.andExpect(jsonPath("$[0].revenue").value(lineTotal.doubleValue()));
	}

	@Test
	void overviewCountsToday() throws Exception {
		mvc.perform(get("/api/admin/overview").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pendingShops").value(1)) // Erode Organics
			.andExpect(jsonPath("$.approvedShops").value(8))
			.andExpect(jsonPath("$.ordersToday").value(1))
			.andExpect(jsonPath("$.packagesSoldToday").value(1))
			.andExpect(jsonPath("$.salesToday").value(packageTotal.doubleValue()));
	}

	@Test
	void onlySevenThirtyOrNinetyDays() throws Exception {
		mvc.perform(get("/api/admin/reports/sales").param("days", "5").cookie(admin)).andExpect(status().isBadRequest());
	}

}

package com.trivoko.order;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** The seller's dashboard numbers (sketch 6), low stock and the sales chart data. */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private org.springframework.cache.CacheManager cacheManager;

	private CheckoutTestSupport t;

	private Cookie kovai;

	private long lowVariant;

	private int lowVariantStock;

	private BigDecimal packageTotal;

	/** Ravi pays for 1 Kovai Sports item -> Kovai has 1 package to pack. */
	@BeforeEach
	void kovaiHasOnePlacedPackage() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		Cookie ravi = Logins.as(mvc, "ravi");
		kovai = Logins.as(mvc, "kovai.sports");
		long address = t.addressOf(2);
		t.addToCart(ravi, t.variantOf(2), 1);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse().getContentAsString();
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi).with(csrf()));
		packageTotal = jdbc.queryForObject("SELECT total FROM packages WHERE seller_id = 2", BigDecimal.class);

		lowVariant = jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.seller_id = 2 AND p.status = 'ACTIVE' ORDER BY v.id DESC LIMIT 1""", Long.class);
		lowVariantStock = t.stock(lowVariant);
		jdbc.update("UPDATE product_variants SET stock = 3 WHERE id = ?", lowVariant);
	}

	@AfterEach
	void clean() {
		var cache = cacheManager.getCache("sellerSales");
		if (cache != null) {
			cache.clear(); // the next test must not see this test's numbers
		}
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", lowVariantStock, lowVariant);
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	@Test
	void dashboardNumbers() throws Exception {
		mvc.perform(get("/api/seller/dashboard").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shopName").value("Kovai Sports"))
			.andExpect(jsonPath("$.toPack").value(1))
			.andExpect(jsonPath("$.toShip").value(0))
			.andExpect(jsonPath("$.lowStock").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.balance").value(0))
			.andExpect(jsonPath("$.todaySales").value(packageTotal.doubleValue()));
	}

	@Test
	void lowStockListsMyVariantsBelowFive() throws Exception {
		mvc.perform(get("/api/seller/products/low-stock").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].variantId", hasItem((int) lowVariant)))
			.andExpect(jsonPath("$[?(@.variantId == " + lowVariant + ")].stock", hasItem(3)));
	}

	@Test
	void salesHaveOnePointPerDay() throws Exception {
		mvc.perform(get("/api/seller/sales").param("days", "7").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(7))
			.andExpect(jsonPath("$[6].total").value(packageTotal.doubleValue())) // today is the last point
			.andExpect(jsonPath("$[6].packages").value(1))
			.andExpect(jsonPath("$[0].total").value(0));
	}

	/** The chart is cached (a minute): the second look does not read every package again. */
	@Test
	void salesAreCached() throws Exception {
		mvc.perform(get("/api/seller/sales").param("days", "30").cookie(kovai)).andExpect(status().isOk());
		var cache = cacheManager.getCache("sellerSales");
		org.assertj.core.api.Assertions.assertThat(cache).isNotNull();
		org.assertj.core.api.Assertions.assertThat(cache.get("2-30-" + java.time.LocalDate.now())).isNotNull();
	}

	@Test
	void onlySevenThirtyOrNinetyDays() throws Exception {
		mvc.perform(get("/api/seller/sales").param("days", "5").cookie(kovai)).andExpect(status().isBadRequest());
	}

	@Test
	void customersHaveNoDashboard() throws Exception {
		mvc.perform(get("/api/seller/dashboard").cookie(Logins.as(mvc, "ravi"))).andExpect(status().isForbidden());
	}

}

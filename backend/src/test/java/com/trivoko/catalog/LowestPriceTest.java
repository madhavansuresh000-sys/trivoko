package com.trivoko.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 9 step 5 - "Lowest price in 30 days" on the product page (from price_history), and the stars on cards.
 * The badge "Lowest in 30 days" shows only when the price really moved in that window and today's price is the
 * lowest - a price that never changed gets no badge.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LowestPriceTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ProductService productService;

	private long volta;

	private long cheapest;

	private BigDecimal price;

	private BigDecimal mrp;

	@BeforeEach
	void setUp() {
		volta = jdbc.queryForObject("SELECT id FROM products WHERE slug = 'volta-v12-5g'", Long.class);
		Map<String, Object> v = jdbc.queryForMap(
				"SELECT id, price, mrp FROM product_variants WHERE product_id = ? ORDER BY price, id LIMIT 1", volta);
		cheapest = ((Number) v.get("id")).longValue();
		price = (BigDecimal) v.get("price");
		mrp = (BigDecimal) v.get("mrp");
	}

	@AfterEach
	void putBack() {
		jdbc.update("UPDATE product_variants SET price = ?, mrp = ? WHERE id = ?", price, mrp, cheapest);
		jdbc.update("UPDATE products SET price_from = ?, mrp_from = ?, rating_avg = NULL, rating_count = 0 WHERE id = ?",
				price, mrp, volta);
		jdbc.update("UPDATE products SET rating_avg = NULL, rating_count = 0");
		jdbc.update("DELETE FROM price_history");
		jdbc.update("DELETE FROM audit_log WHERE action = 'PRICE_CHANGED'");
	}

	private void sellerSets(String newPrice) {
		productService.changePrice(cheapest, new BigDecimal(newPrice), mrp);
	}

	@Test
	void aPriceThatNeverMovedGetsNoBadge() throws Exception {
		mvc.perform(get("/api/products/volta-v12-5g"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lowestPrice30Days").value(price.doubleValue()))
			.andExpect(jsonPath("$.lowestNow").value(false));
	}

	@Test
	void aDropMakesTodayTheLowest() throws Exception {
		sellerSets("11999");

		mvc.perform(get("/api/products/volta-v12-5g"))
			.andExpect(jsonPath("$.priceFrom").value(11999.0))
			.andExpect(jsonPath("$.lowestPrice30Days").value(11999.0))
			.andExpect(jsonPath("$.lowestNow").value(true));
	}

	@Test
	void afterGoingBackUpTheOldLowStaysVisible() throws Exception {
		sellerSets("11999");
		sellerSets("12999");

		mvc.perform(get("/api/products/volta-v12-5g"))
			.andExpect(jsonPath("$.lowestPrice30Days").value(11999.0))
			.andExpect(jsonPath("$.lowestNow").value(false));
	}

	@Test
	void changesOlderThanThirtyDaysAreIgnored() throws Exception {
		jdbc.update("""
				INSERT INTO price_history (variant_id, old_price, new_price, changed_at)
				VALUES (?, 8999.00, ?, NOW(6) - INTERVAL 40 DAY)""", cheapest, price);

		mvc.perform(get("/api/products/volta-v12-5g"))
			.andExpect(jsonPath("$.lowestPrice30Days").value(price.doubleValue()))
			.andExpect(jsonPath("$.lowestNow").value(false));
	}

	@Test
	void cardsAndThePageCarryTheStars() throws Exception {
		jdbc.update("UPDATE products SET rating_avg = 4.5, rating_count = 12");

		mvc.perform(get("/api/products"))
			.andExpect(jsonPath("$.content[0].ratingAvg").value(4.5))
			.andExpect(jsonPath("$.content[0].ratingCount").value(12));
		mvc.perform(get("/api/products/volta-v12-5g"))
			.andExpect(jsonPath("$.ratingAvg").value(4.5))
			.andExpect(jsonPath("$.ratingCount").value(12));
	}

}

package com.trivoko.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.trivoko.catalog.ProductService;
import com.trivoko.support.Logins;

/**
 * Phase 9 step 4 - price-drop alerts that are never spammy. Ravi saved the Volta V12 5G at its "from" price.
 * The seller then changes the cheapest variant's price; the job (called directly here) sends at most ONE alert
 * per real drop of at least 1% below the last price Ravi heard from us.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PriceAlertTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ProductService productService;

	@Autowired
	private PriceAlertService alerts;

	private long volta;

	private long cheapest;

	private BigDecimal price;

	private BigDecimal mrp;

	@BeforeEach
	void raviSavedTheVolta() throws Exception {
		volta = jdbc.queryForObject("SELECT id FROM products WHERE slug = 'volta-v12-5g'", Long.class);
		Map<String, Object> v = jdbc.queryForMap(
				"SELECT id, price, mrp FROM product_variants WHERE product_id = ? ORDER BY price, id LIMIT 1", volta);
		cheapest = ((Number) v.get("id")).longValue();
		price = (BigDecimal) v.get("price");
		mrp = (BigDecimal) v.get("mrp");
		mvc.perform(put("/api/me/wishlist/" + volta).cookie(Logins.as(mvc, "ravi")).with(csrf()));
	}

	@AfterEach
	void putThePriceBack() {
		jdbc.update("UPDATE product_variants SET price = ?, mrp = ? WHERE id = ?", price, mrp, cheapest);
		jdbc.update("UPDATE products SET price_from = ?, mrp_from = ? WHERE id = ?", price, mrp, volta);
		jdbc.update("UPDATE sellers SET status = 'APPROVED' WHERE id = (SELECT seller_id FROM products WHERE id = ?)",
				volta);
		jdbc.update("DELETE FROM price_history");
		jdbc.update("DELETE FROM audit_log WHERE action = 'PRICE_CHANGED'");
		jdbc.update("DELETE FROM wishlist_items");
		jdbc.update("DELETE FROM notifications");
	}

	private void sellerSets(String newPrice) {
		productService.changePrice(cheapest, new BigDecimal(newPrice), mrp);
	}

	private List<Map<String, Object>> alertsForRavi() {
		return jdbc.queryForList("SELECT title, body, link FROM notifications WHERE user_id = 2 AND kind = 'PRICE_DROP'");
	}

	private boolean flagged() {
		return jdbc.queryForObject("SELECT price_changed FROM wishlist_items WHERE product_id = ?", Boolean.class, volta);
	}

	@Test
	void fiveChangesInAMinuteGiveOneAlertWithTheFinalPrice() {
		// the seed price is 12999 - five edits in a row, the last one is what counts
		sellerSets("12500");
		sellerSets("12000");
		sellerSets("12400");
		sellerSets("11999");
		sellerSets("11800");
		assertThat(flagged()).isTrue();

		assertThat(alerts.runOnce()).isEqualTo(1);

		List<Map<String, Object>> sent = alertsForRavi();
		assertThat(sent).hasSize(1);
		assertThat(sent.getFirst().get("body").toString()).contains("₹11,800").contains("₹12,999");
		assertThat(sent.getFirst().get("link")).isEqualTo("/products/volta-v12-5g");
		assertThat(flagged()).isFalse();
		assertThat(jdbc.queryForObject("SELECT last_alerted_price FROM wishlist_items", BigDecimal.class))
			.isEqualByComparingTo("11800");

		// nothing changed since -> the next run sends nothing
		assertThat(alerts.runOnce()).isZero();
		assertThat(alertsForRavi()).hasSize(1);
	}

	@Test
	void upAndBackDownToTheSamePriceIsNotANewDrop() {
		sellerSets("11800");
		alerts.runOnce();
		sellerSets("12500");
		alerts.runOnce();
		sellerSets("11800");
		alerts.runOnce();

		assertThat(alertsForRavi()).hasSize(1);
	}

	@Test
	void aDropOfLessThanOnePercentIsNotWorthAMessage() {
		sellerSets("12900"); // 99 rupees = 0.76% of 12999

		assertThat(alerts.runOnce()).isZero();
		assertThat(flagged()).isFalse();

		sellerSets("12860"); // now 139 below the saved price = 1.07% -> alert
		assertThat(alerts.runOnce()).isEqualTo(1);
	}

	@Test
	void noAlertForAShopThatIsBlocked() {
		jdbc.update("UPDATE sellers SET status = 'BLOCKED' WHERE id = (SELECT seller_id FROM products WHERE id = ?)",
				volta);
		sellerSets("9999");

		assertThat(alerts.runOnce()).isZero();
		assertThat(alertsForRavi()).isEmpty();
	}

	@Test
	void noAlertWhenNothingIsInStock() {
		List<Map<String, Object>> stockBefore = jdbc.queryForList("SELECT id, stock FROM product_variants WHERE product_id = ?",
				volta);
		jdbc.update("UPDATE product_variants SET stock = 0 WHERE product_id = ?", volta);
		try {
			sellerSets("9999");
			assertThat(alerts.runOnce()).isZero();
		}
		finally {
			stockBefore.forEach(row -> jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", row.get("stock"),
					row.get("id")));
		}
	}

	@Test
	void aPriceChangeOfAProductNobodySavedFlagsNothing() {
		long other = jdbc.queryForObject(
				"SELECT MIN(v.id) FROM product_variants v JOIN products p ON p.id = v.product_id WHERE p.id <> ?",
				Long.class, volta);
		BigDecimal otherPrice = jdbc.queryForObject("SELECT price FROM product_variants WHERE id = ?", BigDecimal.class, other);
		BigDecimal otherMrp = jdbc.queryForObject("SELECT mrp FROM product_variants WHERE id = ?", BigDecimal.class, other);
		productService.changePrice(other, otherPrice.subtract(BigDecimal.ONE), otherMrp);
		productService.changePrice(other, otherPrice, otherMrp);

		assertThat(flagged()).isFalse();
	}

}

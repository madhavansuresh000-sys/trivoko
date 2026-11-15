package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** The live demo's night restock (V13 demo_stock): bought stock comes back, sold-out-on-purpose sizes stay sold out. */
@SpringBootTest
@Transactional
class DemoRestockTest {

	@Autowired
	private ProductService productService;

	@Autowired
	private JdbcTemplate jdbc;

	private int stock(long variantId) {
		return jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variantId);
	}

	@Test
	void boughtStockGoesBackUpButNothingIsLowered() {
		// variant 1 starts with 29 (V2 seed), variant 4 is sold out on purpose (0)
		jdbc.update("UPDATE product_variants SET stock = 3 WHERE id = 1");
		jdbc.update("UPDATE product_variants SET stock = 50 WHERE id = 2"); // a seller restocked above the sample level
		int before4 = stock(4);

		assertThat(productService.restockDemoStock()).isGreaterThanOrEqualTo(1);

		assertThat(stock(1)).isEqualTo(29);
		assertThat(stock(2)).isEqualTo(50);
		assertThat(stock(4)).isEqualTo(before4).isZero();
	}

	@Test
	void productsAddedLaterAreNeverTouched() {
		jdbc.update("UPDATE product_variants SET demo_stock = NULL, stock = 1 WHERE id = 1");
		productService.restockDemoStock();
		assertThat(stock(1)).isEqualTo(1);
	}

}

package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway V5 created the order, payment and coupon tables, and Hibernate's entities match them (ddl validate). */
@SpringBootTest
class OrderSchemaTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void theTwoSeedCouponsExist() {
		Map<String, Object> welcome = jdbc.queryForMap("SELECT * FROM coupons WHERE code = 'WELCOME10'");
		assertThat(welcome.get("type")).isEqualTo("PERCENT");
		assertThat(welcome.get("value").toString()).isEqualTo("10.00");
		assertThat(welcome.get("max_discount").toString()).isEqualTo("500.00");
		assertThat(welcome.get("min_order").toString()).isEqualTo("499.00");

		Map<String, Object> flat = jdbc.queryForMap("SELECT * FROM coupons WHERE code = 'FLAT100'");
		assertThat(flat.get("type")).isEqualTo("FLAT");
		assertThat(flat.get("min_order").toString()).isEqualTo("999.00");
	}

	@Test
	void orderTablesAreThere() {
		for (String table : new String[] { "orders", "packages", "order_items", "payments", "processed_payment_events",
				"coupon_redemptions" }) {
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).as(table).isZero();
		}
	}

}

package com.trivoko.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flyway V3: the demo accounts and one owner for every sample shop (spec section 2, decision P2-3).
 */
@SpringBootTest
@Transactional
class AccountsSeedTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private UserRepository users;

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	@Test
	void twelveSeedUsers() {
		assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@trivoko.test'")).isEqualTo(12);
	}

	@Test
	void adminIsAlsoACustomer() {
		User admin = users.findByEmail("admin@trivoko.test").orElseThrow();
		assertThat(admin.getRoles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.ADMIN);
	}

	@Test
	void everyShopHasItsOwnOwner() {
		assertThat(count("SELECT COUNT(*) FROM sellers WHERE user_id IS NULL")).isZero();
		assertThat(count("SELECT COUNT(DISTINCT user_id) FROM sellers")).isEqualTo(count("SELECT COUNT(*) FROM sellers"));
	}

	@Test
	void approvedShopOwnersAreSellers() {
		User kovai = users.findByEmail("kovai.sports@trivoko.test").orElseThrow();
		assertThat(kovai.getRoles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.SELLER);
	}

	/** Erode Organics is still PENDING, so its owner is only a customer for now. */
	@Test
	void pendingShopOwnerIsOnlyACustomer() {
		Long ownerId = jdbc.queryForObject("SELECT user_id FROM sellers WHERE slug = 'erode-organics'", Long.class);
		assertThat(users.findById(ownerId).orElseThrow().getRoles()).containsExactly(Role.CUSTOMER);
	}

}

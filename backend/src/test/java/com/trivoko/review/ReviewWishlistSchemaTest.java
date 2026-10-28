package com.trivoko.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Flyway V12 created reviews and wishlist_items, products got its rating columns, and the entities match (validate). */
@SpringBootTest
@Transactional
class ReviewWishlistSchemaTest {

	@Autowired
	private JdbcTemplate jdbc;

	private long anyProduct() {
		return jdbc.queryForObject("SELECT MIN(id) FROM products", Long.class);
	}

	private long anyUser() {
		return jdbc.queryForObject("SELECT MIN(id) FROM users", Long.class);
	}

	@Test
	void productsStartWithNoRating() {
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE rating_avg IS NOT NULL OR rating_count <> 0",
				Integer.class)).isZero();
	}

	@Test
	void oneReviewPerCustomerPerProduct() {
		String insert = "INSERT INTO reviews (product_id, user_id, rating, title, body) VALUES (?, ?, 5, 'Good', 'Really good shoes')";
		jdbc.update(insert, anyProduct(), anyUser());
		assertThatThrownBy(() -> jdbc.update(insert, anyProduct(), anyUser()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void ratingMustBeOneToFive() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO reviews (product_id, user_id, rating, body) VALUES (?, ?, 6, 'Too many stars here')",
				anyProduct(), anyUser()))
			// MySQL error 3819 (CHECK failed) - Spring files it as a general DataAccessException
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_reviews_rating");
	}

	@Test
	void oneWishlistRowPerCustomerPerProduct() {
		String insert = "INSERT INTO wishlist_items (user_id, product_id, price_when_added) VALUES (?, ?, 999.00)";
		jdbc.update(insert, anyUser(), anyProduct());
		assertThat(jdbc.queryForObject("SELECT price_changed FROM wishlist_items WHERE user_id = ?", Boolean.class,
				anyUser())).isFalse();
		assertThatThrownBy(() -> jdbc.update(insert, anyUser(), anyProduct()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}

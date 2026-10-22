package com.trivoko.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.trivoko.common.BusinessRuleException;

/**
 * Phase 7 "done when": 1,000 customers press "Claim" for 100 units at the same moment -> exactly 100 winners,
 * sold = 100 (never 101), 100 claim rows, 900 hear "Sold out" - and nobody wins twice.
 * The one UPDATE ... WHERE sold < quantity lets MySQL decide one row at a time; Java never reads then writes.
 */
@SpringBootTest
class FlashClaimConcurrencyTest {

	private static final int PEOPLE = 1000;

	private static final int UNITS = 100;

	@Autowired
	private FlashClaimService claims;

	@Autowired
	private JdbcTemplate jdbc;

	private final List<Long> users = new ArrayList<>();

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM flash_sale_claims");
		jdbc.update("DELETE FROM flash_sale_items");
		jdbc.update("DELETE FROM flash_sales");
		for (Long id : users) {
			jdbc.update("DELETE FROM user_roles WHERE user_id = ?", id);
		}
		jdbc.update("DELETE FROM users WHERE email LIKE 'flash%@example.com'");
		users.clear();
	}

	@Test
	void thousandPeopleHundredUnitsExactlyHundredWinners() throws Exception {
		long variant = jdbc.queryForObject("SELECT id FROM product_variants ORDER BY id LIMIT 1", Long.class);
		long seller = jdbc.queryForObject("SELECT p.seller_id FROM product_variants v JOIN products p ON p.id = v.product_id WHERE v.id = ?",
				Long.class, variant);
		jdbc.update("INSERT INTO flash_sales (title, starts_at, ends_at) VALUES ('Load test', ?, ?)",
				LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
		long sale = jdbc.queryForObject("SELECT MAX(id) FROM flash_sales", Long.class);
		jdbc.update("INSERT INTO flash_sale_items (sale_id, variant_id, seller_id, sale_price, quantity) VALUES (?, ?, ?, ?, ?)",
				sale, variant, seller, new BigDecimal("99.00"), UNITS);
		long item = jdbc.queryForObject("SELECT MAX(id) FROM flash_sale_items", Long.class);

		for (int i = 0; i < PEOPLE; i++) {
			jdbc.update("INSERT INTO users (email, password_hash, full_name) VALUES (?, '{noop}!locked', 'Flash Tester')",
					"flash" + i + "@example.com");
			users.add(jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, "flash" + i + "@example.com"));
		}

		AtomicInteger winners = new AtomicInteger();
		AtomicInteger soldOut = new AtomicInteger();
		AtomicInteger otherErrors = new AtomicInteger();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(64);
		for (Long user : users) {
			pool.submit(() -> {
				try {
					start.await(); // everyone presses "Claim" at the same moment
					claims.claim(user, item);
					winners.incrementAndGet();
				}
				catch (BusinessRuleException ex) {
					soldOut.incrementAndGet();
				}
				catch (Exception ex) {
					otherErrors.incrementAndGet();
				}
				return null;
			});
		}
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(120, TimeUnit.SECONDS)).isTrue();

		// the final assertions
		int sold = jdbc.queryForObject("SELECT sold FROM flash_sale_items WHERE id = ?", Integer.class, item);
		int claimRows = jdbc.queryForObject("SELECT COUNT(*) FROM flash_sale_claims WHERE item_id = ?", Integer.class, item);
		int distinctPeople = jdbc.queryForObject("SELECT COUNT(DISTINCT user_id) FROM flash_sale_claims WHERE item_id = ?",
				Integer.class, item);
		assertThat(winners.get()).as("winners").isEqualTo(UNITS);
		assertThat(soldOut.get()).as("told sold out").isEqualTo(PEOPLE - UNITS);
		assertThat(otherErrors.get()).as("unexpected errors").isZero();
		assertThat(sold).as("sold in the database").isEqualTo(UNITS);
		assertThat(claimRows).as("claim rows").isEqualTo(UNITS);
		assertThat(distinctPeople).as("different people").isEqualTo(UNITS);
	}

	@Test
	void oneEagerPersonClickingTwentyTimesWinsOnce() throws Exception {
		long variant = jdbc.queryForObject("SELECT id FROM product_variants ORDER BY id LIMIT 1", Long.class);
		long seller = jdbc.queryForObject("SELECT p.seller_id FROM product_variants v JOIN products p ON p.id = v.product_id WHERE v.id = ?",
				Long.class, variant);
		jdbc.update("INSERT INTO flash_sales (title, starts_at, ends_at) VALUES ('Double click', ?, ?)",
				LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
		long sale = jdbc.queryForObject("SELECT MAX(id) FROM flash_sales", Long.class);
		jdbc.update("INSERT INTO flash_sale_items (sale_id, variant_id, seller_id, sale_price, quantity) VALUES (?, ?, ?, ?, ?)",
				sale, variant, seller, new BigDecimal("99.00"), 50);
		long item = jdbc.queryForObject("SELECT MAX(id) FROM flash_sale_items", Long.class);
		jdbc.update("INSERT INTO users (email, password_hash, full_name) VALUES ('flash-solo@example.com', '{noop}!locked', 'Eager')");
		long user = jdbc.queryForObject("SELECT id FROM users WHERE email = 'flash-solo@example.com'", Long.class);
		users.add(user);

		AtomicInteger wins = new AtomicInteger();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(20);
		for (int i = 0; i < 20; i++) {
			pool.submit(() -> {
				try {
					start.await();
					claims.claim(user, item);
					wins.incrementAndGet();
				}
				catch (Exception expected) {
					// "already claimed" (BusinessRuleException) or the UNIQUE key (DataIntegrityViolation)
				}
				return null;
			});
		}
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

		assertThat(wins.get()).isEqualTo(1);
		// the losing clicks' "+1" was undone together with their failed insert
		assertThat(jdbc.queryForObject("SELECT sold FROM flash_sale_items WHERE id = ?", Integer.class, item)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flash_sale_claims WHERE user_id = ?", Integer.class, user)).isEqualTo(1);
	}

}

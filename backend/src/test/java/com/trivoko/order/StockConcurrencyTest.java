package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import com.trivoko.order.dto.CheckoutDtos.PlaceOrderRequest;
import com.trivoko.order.dto.CheckoutDtos.PreviewRequest;

/**
 * Phase 4 "done when": 100 customers press "Pay" for the LAST 10 units at the same moment -> exactly 10
 * orders, stock 0, never -1. The atomic "UPDATE ... SET stock = stock - 1 WHERE stock >= 1" makes MySQL
 * decide one row at a time; Java never reads-then-writes the stock.
 */
@SpringBootTest
class StockConcurrencyTest {

	private static final int CUSTOMERS = 100;

	private static final int UNITS = 10;

	@Autowired
	private CheckoutService checkout;

	@Autowired
	private JdbcTemplate jdbc;

	private long variant;

	private int stockBefore;

	private final List<Long> users = new ArrayList<>();

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM payments");
		jdbc.update("DELETE FROM order_items");
		jdbc.update("DELETE FROM packages");
		jdbc.update("DELETE FROM orders");
		jdbc.update("DELETE FROM cart_items");
		jdbc.update("DELETE FROM carts");
		jdbc.update("DELETE FROM addresses");
		for (Long id : users) {
			jdbc.update("DELETE FROM user_roles WHERE user_id = ?", id);
			jdbc.update("DELETE FROM users WHERE id = ?", id);
		}
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stockBefore, variant);
	}

	@Test
	void hundredCustomersTenUnitsExactlyTenOrders() throws Exception {
		variant = jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.status = 'ACTIVE' AND p.seller_id = 1 ORDER BY v.id LIMIT 1""", Long.class);
		stockBefore = jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variant);
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", UNITS, variant);

		// 100 customers, each with an address and this one item in the cart
		List<Long> addresses = new ArrayList<>();
		for (int i = 0; i < CUSTOMERS; i++) {
			jdbc.update("INSERT INTO users (email, password_hash, full_name) VALUES (?, '{noop}!locked', 'Load Tester')",
					"load" + i + "@example.com");
			long user = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, "load" + i + "@example.com");
			users.add(user);
			jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'CUSTOMER')", user);
			jdbc.update("""
					INSERT INTO addresses (user_id, name, phone, line1, city, state, pincode, is_default)
					VALUES (?, 'Load Tester', '9000000000', '1 Street', 'Chennai', 'Tamil Nadu', '600001', TRUE)""", user);
			addresses.add(jdbc.queryForObject("SELECT id FROM addresses WHERE user_id = ?", Long.class, user));
			jdbc.update("INSERT INTO carts (user_id) VALUES (?)", user);
			jdbc.update("INSERT INTO cart_items (cart_id, variant_id, quantity) SELECT id, ?, 1 FROM carts WHERE user_id = ?",
					variant, user);
		}
		BigDecimal total = checkout.preview(users.getFirst(), new PreviewRequest(null, null)).grandTotal();

		AtomicInteger placed = new AtomicInteger();
		AtomicInteger soldOut = new AtomicInteger();
		AtomicInteger otherErrors = new AtomicInteger();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS);
		for (int i = 0; i < CUSTOMERS; i++) {
			long user = users.get(i);
			long address = addresses.get(i);
			pool.submit(() -> {
				try {
					start.await(); // everyone presses "Pay" at the same moment
					checkout.place(user, new PlaceOrderRequest(address, null, total));
					placed.incrementAndGet();
				}
				catch (BusinessRuleException ex) {
					soldOut.incrementAndGet(); // "Sorry, ... is sold out / has only N left"
				}
				catch (Exception ex) {
					otherErrors.incrementAndGet();
				}
				return null;
			});
		}
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(2, TimeUnit.MINUTES)).isTrue();

		assertThat(placed.get()).isEqualTo(UNITS);
		assertThat(soldOut.get()).isEqualTo(CUSTOMERS - UNITS);
		assertThat(otherErrors.get()).isZero();
		assertThat(jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variant)).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE status = 'PENDING_PAYMENT'", Integer.class))
			.isEqualTo(UNITS);
	}

}

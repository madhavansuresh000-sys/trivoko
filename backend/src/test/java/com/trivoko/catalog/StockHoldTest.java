package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.trivoko.common.BusinessRuleException;

/**
 * Holding stock at checkout: one atomic UPDATE per line, "... WHERE stock >= :qty". If any line cannot be
 * taken, the caller's transaction rolls back and NO line keeps its hold. (Not @Transactional: each test
 * checks what really reached the database.)
 */
@SpringBootTest
class StockHoldTest {

	@Autowired
	private ProductService productService;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private TransactionTemplate tx;

	private long a;

	private long b;

	private int stockA;

	private int stockB;

	@BeforeEach
	void twoVariantsWithStockFive() {
		var ids = jdbc.queryForList("SELECT id FROM product_variants ORDER BY id DESC LIMIT 2", Long.class);
		a = ids.get(0);
		b = ids.get(1);
		stockA = stock(a);
		stockB = stock(b);
		jdbc.update("UPDATE product_variants SET stock = 5 WHERE id IN (?, ?)", a, b);
	}

	/** Not rolled back by a test transaction, so put the seed numbers back for the other tests. */
	@AfterEach
	void restoreStock() {
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stockA, a);
		jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", stockB, b);
	}

	private int stock(long id) {
		return jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, id);
	}

	@Test
	void holdTakesTheQuantity() {
		tx.executeWithoutResult(s -> productService.holdStock(Map.of(a, 2)));
		assertThat(stock(a)).isEqualTo(3);
	}

	@Test
	void tooMuchNamesTheItemAndHoldsNothing() {
		String name = jdbc.queryForObject("""
				SELECT p.name FROM products p JOIN product_variants v ON v.product_id = p.id WHERE v.id = ?""",
				String.class, a);
		// lines are taken in id order: b (smaller id) IS taken first, then a fails -> b's hold must be undone
		Map<Long, Integer> lines = new LinkedHashMap<>();
		lines.put(a, 6);
		lines.put(b, 2);
		assertThatThrownBy(() -> tx.executeWithoutResult(s -> productService.holdStock(lines)))
			.isInstanceOf(BusinessRuleException.class)
			.hasMessageContaining(name)
			.hasMessageContaining("only 5 left");
		assertThat(stock(a)).isEqualTo(5); // rolled back together
		assertThat(stock(b)).isEqualTo(5);
	}

	@Test
	void soldOutSaysSo() {
		jdbc.update("UPDATE product_variants SET stock = 0 WHERE id = ?", a);
		assertThatThrownBy(() -> tx.executeWithoutResult(s -> productService.holdStock(Map.of(a, 1))))
			.hasMessageContaining("is sold out");
	}

	@Test
	void releaseGivesItBack() {
		tx.executeWithoutResult(s -> productService.releaseStock(Map.of(a, 4)));
		assertThat(stock(a)).isEqualTo(9);
	}

}

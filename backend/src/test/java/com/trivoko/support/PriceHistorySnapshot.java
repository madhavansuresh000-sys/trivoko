package com.trivoko.support;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Saves every price_history row before a test and puts exactly those rows back after it. All tests share one
 * database, and the sample data (Flyway V2) has price history that CatalogSeedTest checks. A test that only ran
 * "DELETE FROM price_history" also wiped those sample rows, so CatalogSeedTest failed whenever it happened to run
 * later (it did on GitHub's Linux machine). Like borrowing a friend's notebook: return it with every page, not empty.
 */
public final class PriceHistorySnapshot {

	private final JdbcTemplate jdbc;

	private final List<Map<String, Object>> rows;

	private PriceHistorySnapshot(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		this.rows = jdbc.queryForList("SELECT id, variant_id, old_price, new_price, changed_at FROM price_history");
	}

	/** Call in @BeforeEach. */
	public static PriceHistorySnapshot take(JdbcTemplate jdbc) {
		return new PriceHistorySnapshot(jdbc);
	}

	/** Call in @AfterEach: removes what the test added and brings back what was there before. */
	public void restore() {
		jdbc.update("DELETE FROM price_history");
		for (Map<String, Object> r : rows) {
			jdbc.update("INSERT INTO price_history (id, variant_id, old_price, new_price, changed_at) VALUES (?, ?, ?, ?, ?)",
					r.get("id"), r.get("variant_id"), r.get("old_price"), r.get("new_price"), r.get("changed_at"));
		}
	}

}

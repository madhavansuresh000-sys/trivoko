package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The Flyway V2 sample data matches the approved sample-data plan
 * (Phase_0_Setup\Phase0_Sketches_and_Sample_Data, section 2).
 */
@SpringBootTest
class CatalogSeedTest {

	@Autowired
	private JdbcTemplate jdbc;

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	@Test
	void tenTopCategoriesEachWithSubCategories() {
		assertThat(count("SELECT COUNT(*) FROM categories WHERE parent_id IS NULL")).isEqualTo(10);
		assertThat(count("SELECT COUNT(*) FROM categories WHERE parent_id IS NOT NULL")).isEqualTo(30);
		// no top category is empty
		assertThat(count("""
				SELECT COUNT(*) FROM categories t WHERE t.parent_id IS NULL
				AND NOT EXISTS (SELECT 1 FROM categories c WHERE c.parent_id = t.id)""")).isZero();
	}

	@Test
	void eightApprovedSellersAndOnePending() {
		assertThat(count("SELECT COUNT(*) FROM sellers WHERE status = 'APPROVED'")).isEqualTo(8);
		assertThat(jdbc.queryForObject("SELECT shop_name FROM sellers WHERE status = 'PENDING'", String.class))
			.isEqualTo("Erode Organics");
	}

	@Test
	void oneHundredTwentyProductsTwelvePerTopCategory() {
		assertThat(count("SELECT COUNT(*) FROM products")).isEqualTo(120);
		assertThat(jdbc.queryForList("""
				SELECT COUNT(*) FROM products p JOIN categories c ON c.id = p.category_id
				GROUP BY c.parent_id""", Integer.class)).hasSize(10).containsOnly(12);
		// products always hang on a sub-category, never on a top category
		assertThat(count("""
				SELECT COUNT(*) FROM products p JOIN categories c ON c.id = p.category_id
				WHERE c.parent_id IS NULL""")).isZero();
	}

	@Test
	void statusesMatchTheApprovalDemo() {
		assertThat(count("SELECT COUNT(*) FROM products WHERE status = 'ACTIVE'")).isEqualTo(116);
		assertThat(count("SELECT COUNT(*) FROM products WHERE status = 'PENDING'")).isEqualTo(3);
		assertThat(count("SELECT COUNT(*) FROM products WHERE status = 'REJECTED' AND rejection_reason IS NOT NULL"))
			.isEqualTo(1);
	}

	@Test
	void aboutThreeHundredVariantsWithRealisticStock() {
		assertThat(count("SELECT COUNT(*) FROM product_variants")).isBetween(270, 330);
		assertThat(count("SELECT COUNT(*) FROM products p WHERE NOT EXISTS "
				+ "(SELECT 1 FROM product_variants v WHERE v.product_id = p.id)")).isZero();
		assertThat(count("SELECT COUNT(*) FROM product_variants WHERE stock = 0")).isGreaterThan(5);
		assertThat(count("SELECT COUNT(*) FROM product_variants WHERE stock BETWEEN 1 AND 4")).isGreaterThan(5);
		assertThat(count("SELECT MIN(price) FROM product_variants")).isEqualTo(199);
		assertThat(count("SELECT MAX(price) FROM product_variants")).isLessThanOrEqualTo(80000);
	}

	@Test
	void priceFromIsTheCheapestVariant() {
		assertThat(count("""
				SELECT COUNT(*) FROM products p
				WHERE p.price_from IS NULL
				   OR p.price_from <> (SELECT MIN(v.price) FROM product_variants v WHERE v.product_id = p.id)
				   OR p.mrp_from < p.price_from""")).isZero();
	}

	@Test
	void someProductsHavePriceHistoryForAlerts() {
		assertThat(count("SELECT COUNT(DISTINCT v.product_id) FROM price_history h "
				+ "JOIN product_variants v ON v.id = h.variant_id")).isGreaterThanOrEqualTo(4);
		// the last change of each history ends at today's price
		assertThat(count("""
				SELECT COUNT(*) FROM price_history h JOIN product_variants v ON v.id = h.variant_id
				WHERE h.changed_at = (SELECT MAX(h2.changed_at) FROM price_history h2 WHERE h2.variant_id = h.variant_id)
				  AND h.new_price <> v.price""")).isZero();
	}

	@Test
	void demoStoryProductsExist() {
		assertThat(count("SELECT COUNT(*) FROM products WHERE slug = 'silicone-case-for-iphone-15' AND status = 'ACTIVE'"))
			.isOne();
		assertThat(count("SELECT COUNT(*) FROM products WHERE slug = 'arc-pulse-anc-headphones' AND status = 'ACTIVE'"))
			.isOne();
		assertThat(count("SELECT COUNT(*) FROM products WHERE slug = 'kovai-run-swift-running-shoes' AND status = 'ACTIVE'"))
			.isOne();
	}

}

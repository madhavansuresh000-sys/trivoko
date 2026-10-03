package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.BadRequestException;
import com.trivoko.common.ResourceNotFoundException;

import jakarta.persistence.EntityManager;

/** ProductService.changePrice: the ONLY way a price changes, so price_history is never missed. */
@SpringBootTest
@Transactional
class PriceChangeTest {

	@Autowired
	private ProductService productService;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	@Test
	void priceChangeIsWrittenToHistory() {
		long variantId = variantId("kaveri-k5", "Black");
		int before = historyRows(variantId);

		productService.changePrice(variantId, new BigDecimal("7999.00"), new BigDecimal("9999.00"));
		em.flush();

		assertThat(price("SELECT price FROM product_variants WHERE id = ?", variantId)).isEqualByComparingTo("7999.00");
		assertThat(historyRows(variantId)).isEqualTo(before + 1);
		assertThat(price("SELECT old_price FROM price_history WHERE variant_id = ? ORDER BY id DESC LIMIT 1", variantId))
			.isEqualByComparingTo("8999.00");
		assertThat(price("SELECT new_price FROM price_history WHERE variant_id = ? ORDER BY id DESC LIMIT 1", variantId))
			.isEqualByComparingTo("7999.00");
		assertThat(price("SELECT price_from FROM products WHERE slug = 'kaveri-k5'")).isEqualByComparingTo("7999.00");
		assertThat(price("SELECT mrp_from FROM products WHERE slug = 'kaveri-k5'")).isEqualByComparingTo("9999.00");
	}

	/** Volta V12: Black/128 = 12,999, Black/256 = 14,999, Blue/128 = 12,999. */
	@Test
	void priceFromFollowsTheCheapestVariant() {
		productService.changePrice(variantId("volta-v12-5g", "Black / 128 GB"), new BigDecimal("15999"), new BigDecimal("17999"));
		em.flush();
		assertThat(price("SELECT price_from FROM products WHERE slug = 'volta-v12-5g'")).isEqualByComparingTo("12999");

		productService.changePrice(variantId("volta-v12-5g", "Blue / 128 GB"), new BigDecimal("16999"), new BigDecimal("17999"));
		em.flush();
		assertThat(price("SELECT price_from FROM products WHERE slug = 'volta-v12-5g'")).isEqualByComparingTo("14999");
	}

	@Test
	void samePriceWritesNoHistory() {
		long variantId = variantId("kaveri-k5", "Black");
		BigDecimal mrp = price("SELECT mrp FROM product_variants WHERE id = ?", variantId);
		int before = historyRows(variantId);

		productService.changePrice(variantId, new BigDecimal("8999.00"), mrp);
		em.flush();

		assertThat(historyRows(variantId)).isEqualTo(before);
	}

	@Test
	void priceAboveMrpIsRefused() {
		long variantId = variantId("kaveri-k5", "Black");
		assertThatThrownBy(() -> productService.changePrice(variantId, new BigDecimal("9000"), new BigDecimal("8000")))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void zeroPriceIsRefused() {
		long variantId = variantId("kaveri-k5", "Black");
		assertThatThrownBy(() -> productService.changePrice(variantId, BigDecimal.ZERO, new BigDecimal("8000")))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void unknownVariantIsNotFound() {
		assertThatThrownBy(() -> productService.changePrice(999_999L, BigDecimal.TEN, BigDecimal.TEN))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	private long variantId(String productSlug, String label) {
		return jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.slug = ? AND v.label = ?""", Long.class, productSlug, label);
	}

	private int historyRows(long variantId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM price_history WHERE variant_id = ?", Integer.class, variantId);
	}

	private BigDecimal price(String sql, Object... args) {
		return jdbc.queryForObject(sql, BigDecimal.class, args);
	}

}

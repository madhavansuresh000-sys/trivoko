package com.trivoko.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One LIVE product as the search module needs it to filter and count (Phase 10): read from MySQL right now, so
 * stock and price are never stale. stockedVariants = how many of its variants have stock left.
 */
public record SearchRow(Long id, String brand, Long categoryId, String categorySlug, String categoryName,
		BigDecimal priceFrom, long stockedVariants, LocalDateTime createdAt) {

	public boolean inStock() {
		return stockedVariants > 0;
	}

}

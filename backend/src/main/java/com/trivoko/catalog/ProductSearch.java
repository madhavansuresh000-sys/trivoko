package com.trivoko.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * The filters of GET /api/products, e.g. ?category=phones&brand=Volta&maxPrice=15000&inStock=true.
 * Every field is optional (null / empty = no filter).
 */
public record ProductSearch(
		String category,
		List<String> brands,
		BigDecimal minPrice,
		BigDecimal maxPrice,
		boolean inStockOnly,
		String seller) {
}

package com.trivoko.catalog.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One size / colour button on the product page.
 * The exact stock is NOT sent (competitors could read it); only "in stock" and,
 * when fewer than 5 are left, "only N left".
 */
public record VariantView(
		Long id,
		String sku,
		String label,
		String size,
		String colour,
		BigDecimal price,
		BigDecimal mrp,
		int discountPercent,
		boolean inStock,
		@JsonInclude(JsonInclude.Include.NON_NULL) Integer onlyLeft) {
}

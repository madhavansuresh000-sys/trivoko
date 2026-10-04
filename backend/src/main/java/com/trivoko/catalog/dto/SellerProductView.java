package com.trivoko.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trivoko.catalog.ProductStatus;

/**
 * A product as its OWN seller sees it: every status, the admin's rejection reason and the EXACT stock
 * (customers only see "in stock" / "only N left").
 */
public record SellerProductView(
		Long id,
		String name,
		String slug,
		String brand,
		String description,
		Long categoryId,
		String categoryName,
		ProductStatus status,
		@JsonInclude(JsonInclude.Include.NON_NULL) String rejectionReason,
		BigDecimal priceFrom,
		BigDecimal mrpFrom,
		List<Variant> variants,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public record Variant(Long id, String sku, String label, String size, String colour, BigDecimal price,
			BigDecimal mrp, int stock) {
	}

}

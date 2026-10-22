package com.trivoko.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trivoko.catalog.ProductStatus;

/** One line of the admin's product list (Phase 6): the product plus WHICH shop sells it. */
public record AdminProductRow(
		Long id,
		String name,
		String slug,
		String brand,
		String categoryName,
		ProductStatus status,
		@JsonInclude(JsonInclude.Include.NON_NULL) String rejectionReason,
		BigDecimal priceFrom,
		int variantCount,
		@JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
		Long sellerId,
		String shopName,
		LocalDateTime createdAt) {
}

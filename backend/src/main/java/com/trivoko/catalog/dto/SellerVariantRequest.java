package com.trivoko.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * One variant in the seller's product form. id = null for a new variant; the id of an existing one
 * when editing. "MRP not below price" is checked in ProductService (it needs both fields).
 */
public record SellerVariantRequest(
		Long id,

		@NotBlank(message = "label is required") @Size(max = 100, message = "label must be at most 100 characters")
		String label,

		@Size(max = 20, message = "size must be at most 20 characters")
		String size,

		@Size(max = 40, message = "colour must be at most 40 characters")
		String colour,

		@NotNull(message = "price is required") @Positive(message = "price must be greater than 0")
		@Digits(integer = 10, fraction = 2, message = "price must have at most 2 decimals")
		BigDecimal price,

		@NotNull(message = "mrp is required") @Positive(message = "mrp must be greater than 0")
		@Digits(integer = 10, fraction = 2, message = "mrp must have at most 2 decimals")
		BigDecimal mrp,

		@NotNull(message = "stock is required") @PositiveOrZero(message = "stock must be 0 or more")
		Integer stock) {
}

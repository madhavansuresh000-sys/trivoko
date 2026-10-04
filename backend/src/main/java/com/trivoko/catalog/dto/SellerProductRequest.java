package com.trivoko.catalog.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The seller's product form (create and edit). Photos come in Phase 5. */
public record SellerProductRequest(
		@NotBlank(message = "name is required") @Size(min = 3, max = 200, message = "name must be 3 to 200 characters")
		String name,

		@NotNull(message = "categoryId is required")
		Long categoryId,

		@NotBlank(message = "brand is required") @Size(max = 80, message = "brand must be at most 80 characters")
		String brand,

		@NotBlank(message = "description is required") @Size(max = 4000, message = "description must be at most 4000 characters")
		String description,

		@NotNull(message = "variants are required")
		@Size(min = 1, max = 10, message = "a product needs 1 to 10 variants")
		List<@Valid SellerVariantRequest> variants) {
}

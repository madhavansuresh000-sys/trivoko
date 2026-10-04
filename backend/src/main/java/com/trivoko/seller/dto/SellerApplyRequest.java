package com.trivoko.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The "Become a seller" form. GSTIN (Indian GST number) is optional: empty, or 15 capital letters/digits. */
public record SellerApplyRequest(
		@NotBlank(message = "shopName is required")
		@Size(min = 3, max = 120, message = "shopName must be 3 to 120 characters")
		String shopName,

		@NotBlank(message = "city is required")
		@Size(min = 2, max = 80, message = "city must be 2 to 80 characters")
		String city,

		@Size(max = 500, message = "description must be at most 500 characters")
		String description,

		@Pattern(regexp = "^$|^[0-9A-Z]{15}$", message = "gstin must be 15 capital letters or digits")
		String gstin) {
}

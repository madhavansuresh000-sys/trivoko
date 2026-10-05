package com.trivoko.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** PUT /api/cart/items/{variantId}: the new quantity of that line (remove = DELETE). */
public record QuantityRequest(
		@NotNull(message = "quantity is required")
		@Min(value = 1, message = "quantity must be 1 to 10")
		@Max(value = 10, message = "quantity must be 1 to 10")
		Integer quantity) {
}

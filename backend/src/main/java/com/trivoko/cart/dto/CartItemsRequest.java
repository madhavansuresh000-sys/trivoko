package com.trivoko.cart.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A guest cart sent from the browser: for /api/cart/preview (show it) and /api/cart/merge (after login).
 * Quantities above 10 are not an error here - the server lowers them (the browser's copy may be old).
 */
public record CartItemsRequest(
		@NotNull(message = "items are required")
		@Size(max = 50, message = "at most 50 items")
		List<@Valid Item> items) {

	public record Item(
			@NotNull(message = "variantId is required") Long variantId,
			@NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") Integer quantity) {
	}

}

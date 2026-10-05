package com.trivoko.cart;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.cart.dto.CartItemsRequest;
import com.trivoko.cart.dto.CartView;
import com.trivoko.cart.dto.QuantityRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The cart. Every call answers with the whole cart, so the page simply shows the latest answer.
 * /preview is public (a guest's cart); everything else is "my cart" and needs login (SecurityConfig).
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "One cart, many sellers")
public class CartController {

	private final CartService cartService;

	@GetMapping
	@Operation(summary = "My cart, grouped into one package per seller")
	public CartView mine(@AuthenticationPrincipal AuthUser user) {
		return cartService.view(user.id());
	}

	@PutMapping("/items/{variantId}")
	@Operation(summary = "Set the quantity of one line (adds it when new)")
	public CartView setQuantity(@AuthenticationPrincipal AuthUser user, @PathVariable Long variantId,
			@Valid @RequestBody QuantityRequest request) {
		return cartService.setQuantity(user.id(), variantId, request.quantity());
	}

	@DeleteMapping("/items/{variantId}")
	@Operation(summary = "Remove one line")
	public CartView remove(@AuthenticationPrincipal AuthUser user, @PathVariable Long variantId) {
		return cartService.remove(user.id(), variantId);
	}

	@PostMapping("/merge")
	@Operation(summary = "After login: add the guest cart from the browser to my cart")
	public CartView merge(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CartItemsRequest request) {
		return cartService.merge(user.id(), request);
	}

	@PostMapping("/preview")
	@Operation(summary = "Price a guest cart (nothing is saved)")
	public CartView preview(@Valid @RequestBody CartItemsRequest request) {
		return cartService.preview(request);
	}

}

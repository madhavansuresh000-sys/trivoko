package com.trivoko.order;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.order.dto.CheckoutDtos.CheckoutPreview;
import com.trivoko.order.dto.CheckoutDtos.PlaceOrderRequest;
import com.trivoko.order.dto.CheckoutDtos.PlacedOrder;
import com.trivoko.order.dto.CheckoutDtos.PreviewRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The checkout page: look (preview), then place the order and go to the payment page. Logged-in only. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Preview and place an order")
public class CheckoutController {

	private final CheckoutService checkoutService;

	@PostMapping("/api/checkout/preview")
	@Operation(summary = "Packages and amounts for my cart, address and coupon (nothing is saved)")
	public CheckoutPreview preview(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PreviewRequest request) {
		return checkoutService.preview(user.id(), request);
	}

	@PostMapping("/api/orders")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Place the order: hold the stock and open the payment page")
	public PlacedOrder place(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PlaceOrderRequest request) {
		return checkoutService.place(user.id(), request);
	}

}

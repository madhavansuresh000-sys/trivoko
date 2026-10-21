package com.trivoko.order.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trivoko.user.dto.AddressView;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** The checkout page's requests and answers (one file: they always change together). */
public final class CheckoutDtos {

	private CheckoutDtos() {
	}

	/** POST /api/checkout/preview - both optional (no address = my default; no coupon = none). */
	public record PreviewRequest(Long addressId, @Size(max = 30, message = "couponCode is too long") String couponCode) {
	}

	/** POST /api/orders. expectedTotal = the total the customer saw; a different total today -> 409. */
	public record PlaceOrderRequest(
			@NotNull(message = "addressId is required") Long addressId,
			@Size(max = 30, message = "couponCode is too long") String couponCode,
			@NotNull(message = "expectedTotal is required") @PositiveOrZero(message = "expectedTotal must be 0 or more")
			@Digits(integer = 10, fraction = 2, message = "expectedTotal must have at most 2 decimals")
			BigDecimal expectedTotal) {
	}

	/** Everything the checkout page shows. coupon = null when no code was given. */
	public record CheckoutPreview(
			AddressView address,
			List<PackageView> packages,
			BigDecimal itemsTotal,
			BigDecimal discountTotal,
			BigDecimal shippingTotal,
			BigDecimal grandTotal,
			@JsonInclude(JsonInclude.Include.NON_NULL) CouponResult coupon,
			List<String> unavailable) {
	}

	/** error != null -> the code was not applied (the page shows the reason under the coupon box). */
	public record CouponResult(String code, BigDecimal discount, String error) {
	}

	public record PackageView(String sellerName, String sellerSlug, List<ItemView> items, BigDecimal itemsTotal,
			BigDecimal discount, BigDecimal shippingFee, BigDecimal total) {
	}

	public record ItemView(Long variantId, String productSlug, String productName, String variantLabel,
			BigDecimal unitPrice, BigDecimal mrp, int quantity, BigDecimal lineTotal, BigDecimal discountShare) {
	}

	/** After "Place order": go to redirectUrl to pay. */
	public record PlacedOrder(String number, BigDecimal grandTotal, String redirectUrl) {
	}

}

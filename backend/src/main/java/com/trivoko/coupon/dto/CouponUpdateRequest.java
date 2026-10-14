package com.trivoko.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The admin's "edit coupon" form (Phase 6). The code and the type are NOT here: customers already know the code,
 * and a FLAT coupon turning into a PERCENT one would be a different coupon.
 */
public record CouponUpdateRequest(
		@NotBlank(message = "description is required") @Size(max = 200, message = "description must be at most 200 characters")
		String description,
		@NotNull(message = "value is required") @Positive(message = "value must be greater than 0")
		@Digits(integer = 10, fraction = 2, message = "value must have at most 2 decimals")
		BigDecimal value,
		@Positive(message = "maxDiscount must be greater than 0")
		BigDecimal maxDiscount,
		@PositiveOrZero(message = "minOrder must be 0 or more")
		BigDecimal minOrder,
		@NotNull(message = "validUntil is required") @Future(message = "validUntil must be in the future")
		LocalDateTime validUntil) {
}

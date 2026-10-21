package com.trivoko.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trivoko.coupon.CouponType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** The admin's "new coupon" form. validFrom empty = from now. maxDiscount is used for PERCENT only. */
public record CouponRequest(
		@NotBlank(message = "code is required")
		@Pattern(regexp = "^\\s*[A-Za-z0-9]{3,30}\\s*$", message = "code must be 3 to 30 letters or digits")
		String code,
		@NotBlank(message = "description is required") @Size(max = 200, message = "description must be at most 200 characters")
		String description,
		@NotNull(message = "type is required")
		CouponType type,
		@NotNull(message = "value is required") @Positive(message = "value must be greater than 0")
		@Digits(integer = 10, fraction = 2, message = "value must have at most 2 decimals")
		BigDecimal value,
		@Positive(message = "maxDiscount must be greater than 0")
		BigDecimal maxDiscount,
		@PositiveOrZero(message = "minOrder must be 0 or more")
		BigDecimal minOrder,
		LocalDateTime validFrom,
		@NotNull(message = "validUntil is required") @Future(message = "validUntil must be in the future")
		LocalDateTime validUntil) {
}

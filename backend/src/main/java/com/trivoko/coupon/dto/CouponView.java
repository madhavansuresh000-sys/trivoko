package com.trivoko.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trivoko.coupon.CouponType;

/** A coupon as the admin list and GET /api/coupons/{code} show it. */
public record CouponView(Long id, String code, String description, CouponType type, BigDecimal value,
		BigDecimal maxDiscount, BigDecimal minOrder, LocalDateTime validFrom, LocalDateTime validUntil, boolean active) {
}

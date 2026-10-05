package com.trivoko.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A discount code the admin creates. The rules live in CouponService. */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Always upper case: "welcome10" and "WELCOME10" are the same coupon. */
	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 200)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CouponType type;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal value;

	@Column(name = "max_discount", precision = 12, scale = 2)
	private BigDecimal maxDiscount;

	@Column(name = "min_order", nullable = false, precision = 12, scale = 2)
	private BigDecimal minOrder = BigDecimal.ZERO;

	@Column(name = "valid_from", nullable = false)
	private LocalDateTime validFrom;

	@Column(name = "valid_until", nullable = false)
	private LocalDateTime validUntil;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

}

package com.trivoko.coupon;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** "Ravi used WELCOME10 on order 12." The UNIQUE (coupon, user) key makes it one use per customer. */
@Entity
@Table(name = "coupon_redemptions")
@Getter
@NoArgsConstructor
public class CouponRedemption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "coupon_id", nullable = false)
	private Long couponId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "redeemed_at", nullable = false)
	private LocalDateTime redeemedAt = LocalDateTime.now();

	public CouponRedemption(Long couponId, Long userId, Long orderId) {
		this.couponId = couponId;
		this.userId = userId;
		this.orderId = orderId;
	}

}

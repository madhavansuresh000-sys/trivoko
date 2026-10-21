package com.trivoko.coupon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.coupon.dto.CouponView;

import lombok.RequiredArgsConstructor;

/** "Does this code exist?" for logged-in customers. How much it takes off comes from the checkout preview. */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

	private final CouponService couponService;

	@GetMapping("/{code}")
	public CouponView describe(@PathVariable String code) {
		return couponService.describe(code);
	}

}

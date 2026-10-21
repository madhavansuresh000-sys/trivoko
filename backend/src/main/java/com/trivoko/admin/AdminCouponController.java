package com.trivoko.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.coupon.CouponService;
import com.trivoko.coupon.dto.CouponRequest;
import com.trivoko.coupon.dto.CouponView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The admin's coupons (ADMIN only, SecurityConfig). The admin pages come in Phase 6. */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

	private final CouponService couponService;

	@GetMapping
	public List<CouponView> list() {
		return couponService.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CouponView create(@Valid @RequestBody CouponRequest request) {
		return couponService.create(request);
	}

}

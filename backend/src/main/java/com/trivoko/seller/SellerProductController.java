package com.trivoko.seller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * A seller's own products. Every method needs the SELLER role (SecurityConfig) AND an APPROVED shop
 * (the class-level @PreAuthorize below), so a pending or blocked shop gets 403 everywhere.
 */
@RestController
@RequestMapping("/api/seller/products")
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
public class SellerProductController {

	private final SellerAccess sellerAccess;

	/** My products. (Filled in by Phase 2 step 6.) */
	@GetMapping
	public PageResponse<Object> mine() {
		sellerAccess.currentSellerId();
		return new PageResponse<>(List.of(), 0, 0, 0, 0, true);
	}

}

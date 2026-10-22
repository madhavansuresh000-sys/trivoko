package com.trivoko.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.PageResponse;
import com.trivoko.order.dto.SellerPackageView;
import com.trivoko.seller.SellerAccess;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * The seller's packing table: /api/seller/** needs the SELLER role (SecurityConfig) AND an APPROVED shop
 * (SellerAccess); each package must be the seller's own (PackageService -> 403 otherwise).
 */
@RestController
@RequestMapping("/api/seller/packages")
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
public class SellerPackageController {

	private final PackageService packages;

	private final SellerAccess sellerAccess;

	public record ShipRequest(
			@NotBlank(message = "trackingNumber is required")
			@Pattern(regexp = "^\\s*[A-Za-z0-9]{6,30}\\s*$", message = "trackingNumber must be 6 to 30 letters or digits")
			String trackingNumber) {
	}

	public record CancelRequest(
			@NotBlank(message = "reason is required")
			@Size(min = 5, max = 200, message = "reason must be 5 to 200 characters")
			String reason) {
	}

	@GetMapping
	public PageResponse<SellerPackageView> mine(@RequestParam(required = false) PackageStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return packages.listForSeller(sellerAccess.currentSellerId(), status,
				PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
	}

	@PostMapping("/{id}/pack")
	public SellerPackageView pack(@PathVariable Long id) {
		return packages.pack(sellerAccess.currentSellerId(), id);
	}

	@PostMapping("/{id}/ship")
	public SellerPackageView ship(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
		return packages.ship(sellerAccess.currentSellerId(), id, request.trackingNumber());
	}

	@PostMapping("/{id}/deliver")
	public SellerPackageView deliver(@PathVariable Long id) {
		return packages.deliver(sellerAccess.currentSellerId(), id);
	}

	@PostMapping("/{id}/cancel")
	public SellerPackageView cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
		return packages.cancelBySeller(sellerAccess.currentSellerId(), id, request.reason());
	}

}

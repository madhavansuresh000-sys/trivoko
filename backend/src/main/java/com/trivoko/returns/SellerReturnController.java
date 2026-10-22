package com.trivoko.returns;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.PageResponse;
import com.trivoko.returns.dto.ReturnDtos.ReturnView;
import com.trivoko.seller.SellerAccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * The seller's side of returns (Phase 8 step 5): SELLER role + an APPROVED shop (same guard as the packing table);
 * only returns of the seller's own shop (404 otherwise).
 */
@RestController
@RequestMapping("/api/seller/returns")
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
@Tag(name = "Returns (seller)", description = "Approve or reject returns of my shop")
public class SellerReturnController {

	private final ReturnService returns;

	private final ReturnPickup pickup;

	private final SellerAccess sellerAccess;

	public record RejectRequest(
			@NotBlank(message = "reason is required")
			@Size(min = 5, max = 300, message = "reason must be 5 to 300 characters")
			String reason) {
	}

	@GetMapping
	@Operation(summary = "Returns of my shop, newest first (optionally one status)")
	public PageResponse<ReturnView> mine(@RequestParam(required = false) ReturnStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return returns.forSeller(sellerAccess.currentSellerId(), status,
				PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), Sort.by(Sort.Direction.DESC, "id")));
	}

	@PostMapping("/{id}/approve")
	@Operation(summary = "Yes - a courier collects the item")
	public ReturnView approve(@PathVariable Long id) {
		return returns.approve(sellerAccess.currentSellerId(), id);
	}

	@PostMapping("/{id}/picked-up")
	@Operation(summary = "The courier has the item - the customer is refunded at once")
	public ReturnView pickedUp(@PathVariable Long id) {
		return pickup.pickedUp(sellerAccess.currentSellerId(), id);
	}

	@PostMapping("/{id}/reject")
	@Operation(summary = "No - with a reason the customer sees (they may escalate once)")
	public ReturnView reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
		return returns.reject(sellerAccess.currentSellerId(), id, request.reason());
	}

}

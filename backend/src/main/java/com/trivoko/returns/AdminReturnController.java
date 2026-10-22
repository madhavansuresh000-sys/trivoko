package com.trivoko.returns;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.PageResponse;
import com.trivoko.returns.dto.ReturnDtos.ReturnView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * The admin's disputes page (Phase 8 step 6). ADMIN only (SecurityConfig: /api/admin/**). Every decision is final
 * and lands in the activity log.
 */
@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@Tag(name = "Returns (admin)", description = "Escalated returns: the final word")
public class AdminReturnController {

	private final ReturnService returns;

	private final ReturnPickup pickup;

	public record ApproveRequest(@Size(max = 300) String note) {
	}

	public record CloseRequest(
			@NotBlank(message = "reason is required")
			@Size(min = 5, max = 300, message = "reason must be 5 to 300 characters")
			String reason) {
	}

	@GetMapping
	@Operation(summary = "Returns in one status - ESCALATED (disputes) by default, oldest first")
	public PageResponse<ReturnView> list(@RequestParam(defaultValue = "ESCALATED") ReturnStatus status,
			@RequestParam(defaultValue = "0") int page) {
		return returns.forAdmin(status, PageRequest.of(Math.max(page, 0), 20, Sort.by(Sort.Direction.ASC, "id")));
	}

	@PostMapping("/{id}/approve")
	@Operation(summary = "Side with the customer: the return goes ahead")
	public ReturnView approve(@PathVariable Long id, @Valid @RequestBody(required = false) ApproveRequest request) {
		return returns.adminApprove(id, request == null ? null : request.note());
	}

	@PostMapping("/{id}/retry-refund")
	@Operation(summary = "Try a failed refund again (same idempotency key: never paid twice)")
	public ReturnView retryRefund(@PathVariable Long id) {
		return pickup.retry(id);
	}

	@PostMapping("/{id}/close")
	@Operation(summary = "Side with the seller: closed for good, with a reason")
	public ReturnView close(@PathVariable Long id, @Valid @RequestBody CloseRequest request) {
		return returns.adminClose(id, request.reason());
	}

}

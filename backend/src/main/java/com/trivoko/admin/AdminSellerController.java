package com.trivoko.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.admin.dto.RejectRequest;
import com.trivoko.common.PageResponse;
import com.trivoko.seller.SellerService;
import com.trivoko.seller.SellerStatus;
import com.trivoko.seller.dto.SellerApplicationView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The admin's shop decisions (ADMIN only, SecurityConfig). Each one is written to the audit log. */
@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class AdminSellerController {

	private final SellerService sellerService;

	/** Paged, 20 per page, oldest first (the PENDING queue is first come, first served). No status = all shops. */
	@GetMapping
	public PageResponse<SellerApplicationView> list(@RequestParam(required = false) SellerStatus status,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
		return sellerService.search(status, q, AdminPages.of(page, Sort.by("id")));
	}

	@PostMapping("/{id}/approve")
	public SellerApplicationView approve(@PathVariable Long id) {
		return sellerService.approve(id);
	}

	@PostMapping("/{id}/reject")
	public SellerApplicationView reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
		return sellerService.reject(id, request.reason());
	}

	/** The reason is shown to the shop owner (same 5-300 character rule as a rejection). */
	@PostMapping("/{id}/block")
	public SellerApplicationView block(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
		return sellerService.block(id, request.reason());
	}

	@PostMapping("/{id}/unblock")
	public SellerApplicationView unblock(@PathVariable Long id) {
		return sellerService.unblock(id);
	}

	/** Small helper shared by the admin lists: page numbers below 0 become 0, always 20 per page. */
	static final class AdminPages {

		static final int SIZE = 20;

		static PageRequest of(int page, Sort sort) {
			return PageRequest.of(Math.max(page, 0), SIZE, sort);
		}

		private AdminPages() {
		}

	}

}

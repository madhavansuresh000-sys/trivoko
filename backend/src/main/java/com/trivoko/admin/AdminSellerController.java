package com.trivoko.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.admin.dto.RejectRequest;
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

	@GetMapping
	public List<SellerApplicationView> list(@RequestParam(defaultValue = "PENDING") SellerStatus status) {
		return sellerService.listByStatus(status);
	}

	@PostMapping("/{id}/approve")
	public SellerApplicationView approve(@PathVariable Long id) {
		return sellerService.approve(id);
	}

	@PostMapping("/{id}/reject")
	public SellerApplicationView reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
		return sellerService.reject(id, request.reason());
	}

	@PostMapping("/{id}/block")
	public SellerApplicationView block(@PathVariable Long id) {
		return sellerService.block(id);
	}

}

package com.trivoko.admin;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.admin.AdminSellerController.AdminPages;
import com.trivoko.admin.dto.RejectRequest;
import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.ProductStatus;
import com.trivoko.catalog.dto.AdminProductRow;
import com.trivoko.catalog.dto.SellerProductView;
import com.trivoko.common.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The admin's product decisions (ADMIN only, SecurityConfig). Each one is written to the audit log. */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final ProductService productService;

	/** Paged, oldest first (first come, first served). No status = every product; q = words in name or brand. */
	@GetMapping
	public PageResponse<AdminProductRow> list(@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
		return productService.searchForAdmin(status, q, AdminPages.of(page, Sort.by("id")));
	}

	/** The whole product (description, variants, stock, photos) so the admin can check it before deciding. */
	@GetMapping("/{id}")
	public SellerProductView get(@PathVariable Long id) {
		return productService.getForSeller(id);
	}

	@PostMapping("/{id}/approve")
	public SellerProductView approve(@PathVariable Long id) {
		return productService.approve(id);
	}

	@PostMapping("/{id}/reject")
	public SellerProductView reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
		return productService.reject(id, request.reason());
	}

}

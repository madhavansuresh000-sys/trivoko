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
import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.ProductStatus;
import com.trivoko.catalog.dto.SellerProductView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The admin's product decisions (ADMIN only, SecurityConfig). Each one is written to the audit log. */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final ProductService productService;

	@GetMapping
	public List<SellerProductView> list(@RequestParam(defaultValue = "PENDING") ProductStatus status) {
		return productService.listByStatus(status);
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

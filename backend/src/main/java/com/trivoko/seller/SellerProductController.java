package com.trivoko.seller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.ProductStatus;
import com.trivoko.catalog.dto.SellerProductRequest;
import com.trivoko.catalog.dto.SellerProductView;
import com.trivoko.common.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * A seller's own products. Two locks on every URL:
 *   1. SecurityConfig: the SELLER role
 *   2. SellerAccess (below): the shop is APPROVED, and for /{id} the product belongs to THIS shop
 * Someone else's product -> 403, even though the URL and the cookie are valid.
 * (A method-level @PreAuthorize replaces the class-level one, so the /{id} methods repeat isActiveSeller.)
 */
@RestController
@RequestMapping("/api/seller/products")
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
public class SellerProductController {

	private static final int MAX_PAGE_SIZE = 48;

	private final SellerAccess sellerAccess;

	private final ProductService productService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SellerProductView create(@Valid @RequestBody SellerProductRequest request) {
		return productService.createDraft(sellerAccess.currentSellerId(), request);
	}

	/** My products only: the query itself is limited to my shop, so other shops' items never appear. */
	@GetMapping
	public PageResponse<SellerProductView> mine(@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "id"));
		return productService.findForSeller(sellerAccess.currentSellerId(), status, pageable);
	}

	@GetMapping("/{id}")
	@PreAuthorize("@sellerAccess.isActiveSeller() and @sellerAccess.ownsProduct(#id)")
	public SellerProductView get(@PathVariable Long id) {
		return productService.getForSeller(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize("@sellerAccess.isActiveSeller() and @sellerAccess.ownsProduct(#id)")
	public SellerProductView update(@PathVariable Long id, @Valid @RequestBody SellerProductRequest request) {
		return productService.updateBySeller(id, request);
	}

	@PostMapping("/{id}/submit")
	@PreAuthorize("@sellerAccess.isActiveSeller() and @sellerAccess.ownsProduct(#id)")
	public SellerProductView submit(@PathVariable Long id) {
		return productService.submit(id);
	}

}

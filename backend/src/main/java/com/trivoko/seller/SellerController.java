package com.trivoko.seller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.seller.dto.SellerProfile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/sellers")
@Tag(name = "Sellers", description = "Public shop pages")
public class SellerController {

	private final SellerService sellerService;

	SellerController(SellerService sellerService) {
		this.sellerService = sellerService;
	}

	@GetMapping("/{slug}")
	@Operation(summary = "A shop page (approved shops only)")
	public SellerProfile get(@PathVariable String slug) {
		return sellerService.getPublicProfile(slug);
	}

}

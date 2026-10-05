package com.trivoko.seller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.seller.dto.SellerApplicationView;
import com.trivoko.seller.dto.SellerApplyRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** "Become a seller": any logged-in user may apply and see their application (SecurityConfig). */
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerApplicationController {

	private final SellerService sellerService;

	@PostMapping("/apply")
	@ResponseStatus(HttpStatus.CREATED)
	public SellerApplicationView apply(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody SellerApplyRequest request) {
		return sellerService.apply(user.id(), request);
	}

	@GetMapping("/application")
	public SellerApplicationView myApplication(@AuthenticationPrincipal AuthUser user) {
		return sellerService.myApplication(user.id());
	}

}

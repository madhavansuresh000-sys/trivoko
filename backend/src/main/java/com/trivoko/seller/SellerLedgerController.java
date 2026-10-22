package com.trivoko.seller;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** My earnings: the balance and the account-book lines (active sellers only). */
@RestController
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
public class SellerLedgerController {

	private final LedgerService ledger;

	private final SellerAccess sellerAccess;

	@GetMapping("/api/seller/ledger")
	public LedgerService.LedgerPage mine(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ledger.page(sellerAccess.currentSellerId(), PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
	}

}

package com.trivoko.seller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.flashsale.FlashSaleService;
import com.trivoko.flashsale.dto.FlashDtos.OfferRequest;
import com.trivoko.flashsale.dto.FlashDtos.SaleSummary;
import com.trivoko.flashsale.dto.FlashDtos.SellerOfferView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** A seller offers their variants in a flash sale (approved shop only; only their own variants). */
@RestController
@RequestMapping("/api/seller/flash-sales")
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
@Tag(name = "Seller - flash sales")
public class SellerFlashSaleController {

	private final SellerAccess sellerAccess;

	private final FlashSaleService saleService;

	/** Every sale (the page shows the ones not ended yet). */
	@GetMapping
	public List<SaleSummary> sales() {
		return saleService.all();
	}

	@GetMapping("/offers")
	public List<SellerOfferView> mine() {
		return saleService.offersOf(sellerAccess.currentSellerId());
	}

	@PostMapping("/{saleId}/offers")
	@ResponseStatus(HttpStatus.CREATED)
	public SellerOfferView offer(@PathVariable Long saleId, @Valid @RequestBody OfferRequest request) {
		return saleService.offer(sellerAccess.currentSellerId(), saleId, request);
	}

	@DeleteMapping("/offers/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@PathVariable Long itemId) {
		saleService.withdraw(sellerAccess.currentSellerId(), itemId);
	}

}

package com.trivoko.flashsale;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.flashsale.dto.FlashDtos.ClaimView;
import com.trivoko.flashsale.dto.FlashDtos.SaleView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** The flash-sale pages: look at the sale (public) and claim a unit (logged in). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Flash sale", description = "Countdown, deals and the atomic claim")
public class FlashSaleController {

	private final FlashSaleService saleService;

	private final FlashClaimService claimService;

	private final ClaimRateLimiter rateLimiter;

	@GetMapping("/api/flash-sales/current")
	@Operation(summary = "The sale running now, or the next one (204 when there is none)")
	public ResponseEntity<SaleView> current() {
		return saleService.current().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/api/flash-sales/{id}")
	@Operation(summary = "One sale with the live 'N left' of every deal")
	public SaleView one(@PathVariable Long id) {
		return saleService.get(id);
	}

	@PostMapping("/api/flash-sales/items/{itemId}/claim")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Claim one unit of a deal (one per person; held 10 minutes for checkout)")
	public ClaimView claim(@AuthenticationPrincipal AuthUser user, @PathVariable Long itemId, HttpServletRequest http) {
		rateLimiter.check(user.id(), http.getRemoteAddr()); // 429 + Retry-After when over the limit
		try {
			return claimService.claim(user.id(), itemId);
		}
		catch (DataIntegrityViolationException twice) {
			// two clicks at the same instant: the UNIQUE key let only one through (and undid the other's +1)
			throw new BusinessRuleException("You already claimed this deal.");
		}
	}

	@GetMapping("/api/me/flash-claims")
	@Operation(summary = "My held and bought deals")
	public List<ClaimView> mine(@AuthenticationPrincipal AuthUser user) {
		return claimService.mine(user.id());
	}

}

package com.trivoko.returns;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.returns.dto.ReturnDtos.RequestReturn;
import com.trivoko.returns.dto.ReturnDtos.ReturnView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The customer's side of returns (Phase 8 step 4). Any logged-in user; only their own lines and returns. */
@RestController
@RequestMapping("/api/me/returns")
@RequiredArgsConstructor
@Tag(name = "Returns (customer)", description = "Ask to return a delivered item, follow it, cancel it")
public class CustomerReturnController {

	private final ReturnService returns;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Return one delivered item (within 7 days, once per item)")
	public ReturnView request(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody RequestReturn request) {
		try {
			return returns.request(user.id(), request);
		}
		catch (DataIntegrityViolationException twice) {
			// two clicks at the same instant: UNIQUE(order_item_id) let only one through
			throw new BusinessRuleException("This item already has a return.");
		}
	}

	@GetMapping
	@Operation(summary = "My returns, newest first")
	public List<ReturnView> mine(@AuthenticationPrincipal AuthUser user) {
		return returns.mine(user.id());
	}

	@GetMapping("/{id}")
	@Operation(summary = "One of my returns with its timeline")
	public ReturnView one(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
		return returns.one(user.id(), id);
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel my return before the seller answers")
	public ReturnView cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
		return returns.cancel(user.id(), id);
	}

}

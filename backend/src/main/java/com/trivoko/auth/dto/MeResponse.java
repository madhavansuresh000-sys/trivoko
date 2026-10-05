package com.trivoko.auth.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trivoko.seller.dto.SellerSummary;

/**
 * Everything the React app needs to know about the logged-in user. The password hash is never sent.
 * seller = their own shop and its status (PENDING, APPROVED ...), left out when they have none.
 */
public record MeResponse(
		Long id,
		String email,
		String fullName,
		List<String> roles,
		@JsonInclude(JsonInclude.Include.NON_NULL) SellerSummary seller) {
}

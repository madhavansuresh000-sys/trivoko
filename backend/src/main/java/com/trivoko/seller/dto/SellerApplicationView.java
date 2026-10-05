package com.trivoko.seller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trivoko.seller.SellerStatus;

/** A shop application as its owner and the admin see it. rejectReason is left out when empty. */
public record SellerApplicationView(
		Long id,
		Long ownerId,
		String shopName,
		String slug,
		String city,
		String description,
		String gstin,
		SellerStatus status,
		@JsonInclude(JsonInclude.Include.NON_NULL) String rejectReason) {
}

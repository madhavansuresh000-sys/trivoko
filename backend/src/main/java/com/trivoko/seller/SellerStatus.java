package com.trivoko.seller;

/** A shop is PENDING until the admin approves it (Phase 2 / 6). Only APPROVED shops are shown. */
public enum SellerStatus {
	PENDING, APPROVED, REJECTED, BLOCKED
}

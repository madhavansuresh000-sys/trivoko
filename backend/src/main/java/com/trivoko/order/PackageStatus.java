package com.trivoko.order;

/** One seller's box. PACKED, SHIPPED, DELIVERED and CANCELLED arrive in Phase 5 (seller dashboard). */
public enum PackageStatus {
	PENDING_PAYMENT, PLACED, EXPIRED
}

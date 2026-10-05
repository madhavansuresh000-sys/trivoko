package com.trivoko.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * One seller's box, as a STATE MACHINE (Master Plan 4.4, same idea as EventHub's EventStatus.nextAllowed):
 *
 * <pre>
 *   PENDING_PAYMENT --paid--> PLACED --seller--> PACKED --tracking no.--> SHIPPED --courier--> DELIVERED
 *         └-- 10 min --> EXPIRED      └──── customer or seller, before shipping ────> CANCELLED
 *                        (EXPIRED --paid late, stock still there--> PLACED)
 * </pre>
 *
 * allowedNext() is the ONE place that says which move is legal; every other move is refused (409).
 */
public enum PackageStatus {

	PENDING_PAYMENT, PLACED, PACKED, SHIPPED, DELIVERED, CANCELLED, EXPIRED;

	public Set<PackageStatus> allowedNext() {
		return switch (this) {
			case PENDING_PAYMENT -> EnumSet.of(PLACED, EXPIRED);
			case PLACED -> EnumSet.of(PACKED, CANCELLED);
			case PACKED -> EnumSet.of(SHIPPED, CANCELLED);
			case SHIPPED -> EnumSet.of(DELIVERED); // too late to cancel: that is a return (Phase 8)
			case EXPIRED -> EnumSet.of(PLACED); // only a LATE payment while the stock is still there (Phase 4)
			case DELIVERED, CANCELLED -> EnumSet.noneOf(PackageStatus.class);
		};
	}

	public boolean canMoveTo(PackageStatus next) {
		return allowedNext().contains(next);
	}

}

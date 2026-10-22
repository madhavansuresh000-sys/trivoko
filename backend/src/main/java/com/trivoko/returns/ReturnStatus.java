package com.trivoko.returns;

import java.util.EnumSet;
import java.util.Set;

/**
 * The life of a return, as a STATE MACHINE (spec: Phase_8_Returns_and_Refunds/Phase8_Design_Spec_2026-10-22_0935.md,
 * section 2) - the same idea as order.PackageStatus:
 *
 * <pre>
 *  REQUESTED --seller/job--> APPROVED --seller--> PICKED_UP --refund ok--> REFUNDED
 *      |                                             \--refund fails--> REFUND_FAILED --admin retry--> REFUNDED
 *      +--seller--> REJECTED --customer, once--> ESCALATED --admin--> APPROVED | CLOSED
 *      +--customer--> CANCELLED
 * </pre>
 *
 * allowedNext() is the ONE place that says which move is legal; every other move is refused (409).
 * "Escalate only once" needs no extra check: ESCALATED can never go back to REJECTED, so there is no second time.
 */
public enum ReturnStatus {

	REQUESTED, APPROVED, REJECTED, ESCALATED, PICKED_UP, REFUNDED, REFUND_FAILED, CLOSED, CANCELLED;

	public Set<ReturnStatus> allowedNext() {
		return switch (this) {
			case REQUESTED -> EnumSet.of(APPROVED, REJECTED, CANCELLED); // seller decides, or the customer changes their mind
			case APPROVED -> EnumSet.of(PICKED_UP); // the courier collects it (no cancel: it is already on its way)
			case PICKED_UP -> EnumSet.of(REFUNDED, REFUND_FAILED); // the money call works or fails - nothing else
			case REFUND_FAILED -> EnumSet.of(REFUNDED); // the admin retries with the SAME idempotency key
			case REJECTED -> EnumSet.of(ESCALATED); // the customer may ask the admin once
			case ESCALATED -> EnumSet.of(APPROVED, CLOSED); // the admin's word is final
			case REFUNDED, CLOSED, CANCELLED -> EnumSet.noneOf(ReturnStatus.class); // end states
		};
	}

	public boolean canMoveTo(ReturnStatus next) {
		return allowedNext().contains(next);
	}

}

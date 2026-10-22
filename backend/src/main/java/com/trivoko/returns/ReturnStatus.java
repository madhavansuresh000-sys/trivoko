package com.trivoko.returns;

/**
 * The life of a return (spec: Phase_8_Returns_and_Refunds/Phase8_Design_Spec_2026-10-22_0935.md, section 2).
 *
 * <pre>
 *  REQUESTED --seller/job--> APPROVED --seller--> PICKED_UP --refund ok--> REFUNDED
 *      |                                             \--refund fails--> REFUND_FAILED --admin retry--> REFUNDED
 *      +--seller--> REJECTED --customer, once--> ESCALATED --admin--> APPROVED | CLOSED
 *      +--customer--> CANCELLED
 * </pre>
 *
 * The values are listed here so the tables and entities can be built (Phase 8 step 1).
 * ★ Phase 8 step 2 is Madhavan's: add {@code allowedNext()} (and {@code canMoveTo(next)}) here, the same shape as
 * {@code order.PackageStatus} - then {@code ReturnService.moveTo(...)} uses it for every move.
 */
public enum ReturnStatus {

	REQUESTED, APPROVED, REJECTED, ESCALATED, PICKED_UP, REFUNDED, REFUND_FAILED, CLOSED, CANCELLED

}

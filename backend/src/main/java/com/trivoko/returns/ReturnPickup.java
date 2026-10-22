package com.trivoko.returns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.trivoko.returns.dto.ReturnDtos.ReturnView;

import lombok.RequiredArgsConstructor;

/**
 * "Picked up" and "Retry refund" (Phase 8 step 7). On purpose NOT @Transactional: each ReturnService call below
 * is its own transaction, so
 * <ul>
 * <li>PICKED_UP is saved even if the refund then fails (the item really is with the seller);</li>
 * <li>a failed refund rolls back only its own work (status, stock, ledger) - and we can still write
 * REFUND_FAILED in a fresh transaction for the admin to see.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReturnPickup {

	private static final Logger log = LoggerFactory.getLogger(ReturnPickup.class);

	private final ReturnService returns;

	/** Seller: the courier has the item -> refund straight away. */
	public ReturnView pickedUp(Long sellerId, Long returnId) {
		returns.markPickedUp(sellerId, returnId);
		return tryRefund(returnId);
	}

	/** Admin: try a failed refund again (same idempotency key - Stripe will not pay twice). */
	public ReturnView retry(Long returnId) {
		returns.checkRetryable(returnId);
		return tryRefund(returnId);
	}

	private ReturnView tryRefund(Long returnId) {
		try {
			return returns.refund(returnId);
		}
		catch (RuntimeException e) {
			log.warn("Refund for return {} failed: {}", returnId, e.getMessage());
			return returns.markRefundFailed(returnId, e.getMessage());
		}
	}

}

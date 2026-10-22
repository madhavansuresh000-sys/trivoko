package com.trivoko.returns;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** The return state machine on its own - plain JUnit, no Spring, no database (fast). */
class ReturnStatusTest {

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"REQUESTED, APPROVED", "REQUESTED, REJECTED", "REQUESTED, CANCELLED",
			"APPROVED, PICKED_UP",
			"PICKED_UP, REFUNDED", "PICKED_UP, REFUND_FAILED",
			"REFUND_FAILED, REFUNDED",
			"REJECTED, ESCALATED",
			"ESCALATED, APPROVED", "ESCALATED, CLOSED" })
	void legalMoves(ReturnStatus from, ReturnStatus to) {
		assertThat(from.canMoveTo(to)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1} is refused")
	@CsvSource({
			"REFUNDED, APPROVED", // money already back: nothing may restart it
			"REFUNDED, REFUNDED", // a second refund
			"CLOSED, ESCALATED", // the admin's no is final
			"PICKED_UP, CANCELLED", // the item is already with the seller
			"APPROVED, CANCELLED",
			"REJECTED, APPROVED", // the customer must escalate first; the seller cannot flip-flop
			"REQUESTED, PICKED_UP", // no pickup without approval
			"REQUESTED, REFUNDED", // no money without the item
			"ESCALATED, REJECTED" }) // no way back = escalate only once
	void illegalMoves(ReturnStatus from, ReturnStatus to) {
		assertThat(from.canMoveTo(to)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(value = ReturnStatus.class, names = { "REFUNDED", "CLOSED", "CANCELLED" })
	void endStatesGoNowhere(ReturnStatus end) {
		assertThat(end.allowedNext()).isEmpty();
	}

	@Test
	void noStateMayMoveToItself() {
		for (ReturnStatus s : ReturnStatus.values()) {
			assertThat(s.canMoveTo(s)).as(s.name()).isFalse();
		}
	}

	@Test
	void theOnlyWayToRefundedIsThroughPickup() {
		EnumSet<ReturnStatus> canReachRefunded = EnumSet.noneOf(ReturnStatus.class);
		for (ReturnStatus s : ReturnStatus.values()) {
			if (s.canMoveTo(ReturnStatus.REFUNDED)) {
				canReachRefunded.add(s);
			}
		}
		assertThat(canReachRefunded).containsExactlyInAnyOrder(ReturnStatus.PICKED_UP, ReturnStatus.REFUND_FAILED);
	}

}

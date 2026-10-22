package com.trivoko.order;

import static com.trivoko.order.PackageStatus.CANCELLED;
import static com.trivoko.order.PackageStatus.DELIVERED;
import static com.trivoko.order.PackageStatus.EXPIRED;
import static com.trivoko.order.PackageStatus.PACKED;
import static com.trivoko.order.PackageStatus.PENDING_PAYMENT;
import static com.trivoko.order.PackageStatus.PLACED;
import static com.trivoko.order.PackageStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The package state machine: the ONLY moves that are allowed (everything else is refused with 409). */
class PackageStatusTest {

	@Test
	void allowedMoves() {
		assertThat(PENDING_PAYMENT.allowedNext()).containsExactlyInAnyOrder(PLACED, EXPIRED);
		assertThat(PLACED.allowedNext()).containsExactlyInAnyOrder(PACKED, CANCELLED);
		assertThat(PACKED.allowedNext()).containsExactlyInAnyOrder(SHIPPED, CANCELLED);
		assertThat(SHIPPED.allowedNext()).containsExactly(DELIVERED); // too late to cancel: returns (Phase 8)
	}

	@Test
	void endStatesGoNowhere() {
		assertThat(DELIVERED.allowedNext()).isEmpty();
		assertThat(CANCELLED.allowedNext()).isEmpty();
	}

	/** Phase 4 late payment: paid after the 10 minutes and the stock is still there -> placed after all. */
	@Test
	void expiredCanOnlyComeBackAsPlaced() {
		assertThat(EXPIRED.allowedNext()).containsExactly(PLACED);
	}

	@Test
	void canMoveToAnswersTheSameQuestion() {
		assertThat(PLACED.canMoveTo(SHIPPED)).isFalse(); // pack first
		assertThat(PACKED.canMoveTo(SHIPPED)).isTrue();
	}

}

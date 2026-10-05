package com.trivoko.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** 5 wrong passwords = locked for 15 minutes. A movable clock means no real waiting. */
class LoginLockTest {

	/** A clock the test can move forward. */
	private static final class MovableClock extends Clock {

		private Instant now = Instant.parse("2026-10-04T10:00:00Z");

		void plus(Duration d) {
			now = now.plus(d);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

	}

	private final MovableClock clock = new MovableClock();

	private final LoginAttemptService service = new LoginAttemptService(5, Duration.ofMinutes(15), clock);

	private void fail(int times, String ip) {
		for (int i = 0; i < times; i++) {
			service.failed("ravi@trivoko.test", ip);
		}
	}

	@Test
	void fourWrongPasswordsAreStillAllowed() {
		fail(4, "1.1.1.1");
		assertThatCode(() -> service.checkAllowed("ravi@trivoko.test", "1.1.1.1")).doesNotThrowAnyException();
	}

	@Test
	void fifthWrongPasswordLocks() {
		fail(5, "1.1.1.1");
		assertThatThrownBy(() -> service.checkAllowed("ravi@trivoko.test", "1.1.1.1"))
			.isInstanceOf(TooManyLoginAttemptsException.class)
			.hasMessageContaining("15 minutes");
	}

	@Test
	void lockEndsAfterFifteenMinutes() {
		fail(5, "1.1.1.1");
		clock.plus(Duration.ofMinutes(15).plusSeconds(1));
		assertThatCode(() -> service.checkAllowed("ravi@trivoko.test", "1.1.1.1")).doesNotThrowAnyException();
	}

	/** Locking by email + address: a stranger cannot lock Ravi out from another computer. */
	@Test
	void otherAddressIsNotLocked() {
		fail(5, "1.1.1.1");
		assertThatCode(() -> service.checkAllowed("ravi@trivoko.test", "2.2.2.2")).doesNotThrowAnyException();
	}

	@Test
	void rightPasswordForgetsEarlierMistakes() {
		fail(4, "1.1.1.1");
		service.succeeded("ravi@trivoko.test", "1.1.1.1");
		fail(4, "1.1.1.1");
		assertThatCode(() -> service.checkAllowed("ravi@trivoko.test", "1.1.1.1")).doesNotThrowAnyException();
	}

}

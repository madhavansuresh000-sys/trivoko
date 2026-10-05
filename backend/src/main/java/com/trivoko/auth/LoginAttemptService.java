package com.trivoko.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stops password guessing: after 5 wrong passwords for the same email from the same address,
 * login is locked for 15 minutes (HTTP 429), even with the right password.
 *
 * Why email + address? Locking by email alone would let anyone lock YOUR account by typing wrong
 * passwords on purpose; locking by address alone would block a whole office or college Wi-Fi.
 * Kept in memory: fine for one server. With several servers (Phase 12, if needed) this would move to Redis.
 */
@Service
public class LoginAttemptService {

	private static final int CLEANUP_ABOVE = 10_000;

	/** Wrong tries so far, and until when the key is locked (null = not locked). */
	private record Attempts(int failures, Instant lockedUntil) {
	}

	private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

	private final int maxFailures;

	private final Duration lockTime;

	private final Clock clock;

	@Autowired
	public LoginAttemptService(@Value("${app.login.max-failures}") int maxFailures,
			@Value("${app.login.lock-time}") Duration lockTime) {
		this(maxFailures, lockTime, Clock.systemUTC());
	}

	/** For tests: a clock we can move forward. */
	LoginAttemptService(int maxFailures, Duration lockTime, Clock clock) {
		this.maxFailures = maxFailures;
		this.lockTime = lockTime;
		this.clock = clock;
	}

	/** Call BEFORE checking the password. Throws if this email + address is locked. */
	public void checkAllowed(String email, String address) {
		Attempts a = attempts.get(key(email, address));
		if (a != null && a.lockedUntil() != null) {
			Duration left = Duration.between(clock.instant(), a.lockedUntil());
			if (left.isPositive()) {
				throw new TooManyLoginAttemptsException(left);
			}
			attempts.remove(key(email, address)); // lock time is over: start again
		}
	}

	/** Wrong password: count it; the 5th one locks. */
	public void failed(String email, String address) {
		if (attempts.size() > CLEANUP_ABOVE) {
			removeOld();
		}
		attempts.compute(key(email, address), (k, a) -> {
			int failures = (a == null ? 0 : a.failures()) + 1;
			return failures >= maxFailures
					? new Attempts(0, clock.instant().plus(lockTime))
					: new Attempts(failures, null);
		});
	}

	/** Right password: forget the earlier mistakes. */
	public void succeeded(String email, String address) {
		attempts.remove(key(email, address));
	}

	private void removeOld() {
		Instant now = clock.instant();
		attempts.values().removeIf(a -> a.lockedUntil() == null || a.lockedUntil().isBefore(now));
	}

	private static String key(String email, String address) {
		return email + "|" + address;
	}

}

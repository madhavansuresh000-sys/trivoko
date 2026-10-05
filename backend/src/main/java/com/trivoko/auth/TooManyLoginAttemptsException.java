package com.trivoko.auth;

import java.time.Duration;

/** Login is locked for a while after too many wrong passwords (HTTP 429 Too Many Requests). */
public class TooManyLoginAttemptsException extends RuntimeException {

	private final Duration retryAfter;

	public TooManyLoginAttemptsException(Duration retryAfter) {
		super("Too many wrong passwords. Please wait " + minutes(retryAfter) + " and try again.");
		this.retryAfter = retryAfter;
	}

	public Duration getRetryAfter() {
		return retryAfter;
	}

	private static String minutes(Duration d) {
		long m = Math.max(1, (d.toSeconds() + 59) / 60); // round up: never say "0 minutes"
		return m == 1 ? "1 minute" : m + " minutes";
	}

}

package com.trivoko.common;

/** Too many tries in a short time (HTTP 429 with a Retry-After header). */
public class TooManyRequestsException extends RuntimeException {

	private final long retryAfterSeconds;

	public TooManyRequestsException(String message, long retryAfterSeconds) {
		super(message);
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

}

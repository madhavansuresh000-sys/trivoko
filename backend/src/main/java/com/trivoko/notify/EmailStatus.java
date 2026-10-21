package com.trivoko.notify;

/** NONE = bell only. PENDING = waiting to be emailed (or retried). FAILED = gave up after 5 tries. */
public enum EmailStatus {
	NONE, PENDING, SENT, FAILED
}

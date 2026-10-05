package com.trivoko.payment;

/** CREATED (page open) -> PAID, or EXPIRED (10 minutes), or REFUNDED (paid too late, stock gone). */
public enum PaymentStatus {
	CREATED, PAID, REFUNDED, EXPIRED
}

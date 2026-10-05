package com.trivoko.order;

/**
 * <pre>
 *   PENDING_PAYMENT --paid--> PAID
 *         └--10 minutes, not paid--> EXPIRED (stock back; a late payment re-takes it or is refunded)
 * </pre>
 */
public enum OrderStatus {
	PENDING_PAYMENT, PAID, EXPIRED
}

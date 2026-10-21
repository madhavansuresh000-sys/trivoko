package com.trivoko.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small money helpers. TriVoKo stores rupees as DECIMAL(12,2); Stripe wants whole paise (₹1 = 100 paise):
 *   ₹15,400.00 -> 1540000
 * Every rounding is to 2 places, HALF_UP (₹99.805 -> ₹99.81), the way a shop bill rounds.
 */
public final class Money {

	public static final BigDecimal ZERO = new BigDecimal("0.00");

	private Money() {
	}

	public static BigDecimal round(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	public static long toPaise(BigDecimal rupees) {
		if (rupees == null) {
			throw new IllegalArgumentException("amount is required");
		}
		return round(rupees).movePointRight(2).longValueExact();
	}

}

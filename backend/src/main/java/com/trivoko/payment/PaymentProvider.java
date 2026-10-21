package com.trivoko.payment;

/** FAKE = the built-in test page (development, no keys). STRIPE = real Stripe Checkout in test mode. */
public enum PaymentProvider {
	FAKE, STRIPE
}

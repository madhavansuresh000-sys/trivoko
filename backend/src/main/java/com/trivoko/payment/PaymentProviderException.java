package com.trivoko.payment;

/** The payment company could not be reached or refused (shown as 503 "please try again"). */
public class PaymentProviderException extends RuntimeException {

	public PaymentProviderException(String message, Throwable cause) {
		super(message, cause);
	}

}

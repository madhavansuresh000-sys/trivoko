package com.trivoko.common;

/** A request value is wrong, e.g. an unknown sort field or max price below min price (HTTP 400). */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

}

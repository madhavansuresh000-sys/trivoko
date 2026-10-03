package com.trivoko.common;

/** The request is valid but breaks a shop rule, e.g. "only 2 left in stock" (HTTP 409). */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}

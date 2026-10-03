package com.trivoko.common;

/** Something asked for does not exist, e.g. "Product iphone-99 not found" (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String what, Object id) {
		super(what + " " + id + " not found");
	}

}

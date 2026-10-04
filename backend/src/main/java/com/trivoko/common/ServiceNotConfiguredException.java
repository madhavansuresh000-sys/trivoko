package com.trivoko.common;

/** A feature cannot work because its keys are missing, e.g. no Cloudinary keys in .env (HTTP 503). */
public class ServiceNotConfiguredException extends RuntimeException {

	public ServiceNotConfiguredException(String message) {
		super(message);
	}

}

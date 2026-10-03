package com.trivoko.catalog.dto;

/**
 * Everything the browser needs to send ONE photo straight to Cloudinary.
 * The API secret is not here: only the signature made with it.
 */
public record UploadSignature(
		String cloudName,
		String apiKey,
		long timestamp,
		String signature,
		String folder,
		String allowedFormats,
		long maxFileSize,
		String uploadUrl) {
}

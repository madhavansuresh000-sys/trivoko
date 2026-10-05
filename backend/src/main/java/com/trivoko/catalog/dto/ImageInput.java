package com.trivoko.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One photo in the seller's list: the Cloudinary URL (uploaded by the browser) and a short description. */
public record ImageInput(
		@NotBlank(message = "url is required") @Size(max = 500, message = "url is too long") String url,
		@Size(max = 200, message = "altText must be at most 200 characters") String altText) {
}

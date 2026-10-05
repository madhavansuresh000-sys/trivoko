package com.trivoko.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why the admin says no. Shown to the seller, so it must be a real sentence. */
public record RejectRequest(
		@NotBlank(message = "reason is required")
		@Size(min = 5, max = 300, message = "reason must be 5 to 300 characters")
		String reason) {
}

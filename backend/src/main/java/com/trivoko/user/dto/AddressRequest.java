package com.trivoko.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the address form sends (add and edit). Indian format: 10-digit phone, 6-digit pincode. */
public record AddressRequest(
		@NotBlank(message = "name is required") @Size(max = 100, message = "name must be at most 100 characters")
		String name,

		@NotBlank(message = "phone is required") @Pattern(regexp = "\\d{10}", message = "phone must be 10 digits")
		String phone,

		@NotBlank(message = "line1 is required") @Size(max = 160, message = "line1 must be at most 160 characters")
		String line1,

		@Size(max = 160, message = "line2 must be at most 160 characters")
		String line2,

		@NotBlank(message = "city is required") @Size(max = 80, message = "city must be at most 80 characters")
		String city,

		@NotBlank(message = "state is required") @Size(max = 80, message = "state must be at most 80 characters")
		String state,

		@NotBlank(message = "pincode is required") @Pattern(regexp = "\\d{6}", message = "pincode must be 6 digits")
		String pincode) {
}

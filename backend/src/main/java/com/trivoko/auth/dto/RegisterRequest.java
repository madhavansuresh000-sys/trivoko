package com.trivoko.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the Register form sends. Every new account is a CUSTOMER. */
public record RegisterRequest(
		@NotBlank(message = "email is required")
		@Email(message = "email must be a valid email address")
		@Size(max = 160, message = "email must be at most 160 characters")
		String email,

		// BCrypt only uses the first 72 bytes, so longer passwords are not allowed
		@NotBlank(message = "password is required")
		@Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
		String password,

		@NotBlank(message = "fullName is required")
		@Size(min = 3, max = 100, message = "fullName must be 3 to 100 characters")
		String fullName,

		@Pattern(regexp = "^$|^\\d{10}$", message = "phone must be 10 digits")
		String phone) {

	/** Spaces around the email are allowed (" Ravi@x.com "); @Email checks the trimmed value. */
	public RegisterRequest {
		email = email == null ? null : email.trim();
	}

}

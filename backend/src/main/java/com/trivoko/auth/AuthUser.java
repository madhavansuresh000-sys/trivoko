package com.trivoko.auth;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.trivoko.user.Role;

/**
 * The logged-in person for THIS request. Who they are comes from the JWT cookie; their roles come
 * fresh from the database (JwtCookieFilter), so an approval or a block works at once.
 * Controllers ask for it with @AuthenticationPrincipal AuthUser user.
 */
public record AuthUser(Long id, String email, String fullName, Set<Role> roles) {

	/** The user of the current request, or empty for a visitor who is not logged in. */
	public static Optional<AuthUser> current() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
			return Optional.of(user);
		}
		return Optional.empty();
	}

	public boolean has(Role role) {
		return roles.contains(role);
	}

}

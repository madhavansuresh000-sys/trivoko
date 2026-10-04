package com.trivoko.auth;

import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.trivoko.auth.dto.MeResponse;
import com.trivoko.auth.dto.RegisterRequest;
import com.trivoko.seller.SellerService;
import com.trivoko.user.User;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Creating accounts and describing the logged-in user. Checking the password itself is done by
 * Spring Security, which asks loadUserByUsername() for the stored BCrypt hash.
 */
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

	private final UserService userService;

	private final SellerService sellerService;

	public AuthUser register(RegisterRequest request) {
		User user = userService.register(request.email(), request.password(), request.fullName(), request.phone());
		return toAuthUser(user);
	}

	/** After a successful password check: the user, ready to be put on a JWT. */
	public AuthUser forToken(String email) {
		return toAuthUser(userService.findByEmail(email).orElseThrow());
	}

	public MeResponse me(AuthUser user) {
		return new MeResponse(user.id(), user.email(), user.fullName(),
				user.roles().stream().map(Enum::name).sorted().toList(),
				sellerService.findByOwner(user.id()).orElse(null));
	}

	/** Spring Security asks this during login. disabled = blocked users cannot log in. */
	@Override
	public UserDetails loadUserByUsername(String email) {
		User user = userService.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("No user with this email"));
		return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
			.password(user.getPasswordHash())
			.disabled(!user.isEnabled())
			.roles(user.getRoles().stream().map(Enum::name).toArray(String[]::new))
			.build();
	}

	private static AuthUser toAuthUser(User user) {
		return new AuthUser(user.getId(), user.getEmail(), user.getFullName(), Set.copyOf(user.getRoles()));
	}

}

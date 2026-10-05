package com.trivoko.user;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.BadRequestException;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** The user module's counter: other modules (auth, seller, admin) come here, never to the repositories. */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	/**
	 * New CUSTOMER account. The password is stored only as a BCrypt hash: a one-way "fingerprint" with
	 * a random salt, so even we cannot see the real password.
	 */
	@Transactional
	public User register(String email, String rawPassword, String fullName, String phone) {
		String normalized = normalizeEmail(email);
		if (users.existsByEmail(normalized)) {
			throw new BusinessRuleException("An account with this email already exists. Try logging in.");
		}
		// DevDataSeeder gives every @trivoko.test account the public demo password, so a real sign-up there
		// could be taken over by anyone who knows it
		if (normalized.endsWith(DevDataSeeder.DEMO_DOMAIN)) {
			throw new BadRequestException("Emails ending in " + DevDataSeeder.DEMO_DOMAIN + " are reserved for the demo accounts.");
		}
		User user = new User(normalized, passwordEncoder.encode(rawPassword), fullName.trim(), blankToNull(phone));
		user.getRoles().add(Role.CUSTOMER);
		return users.save(user);
	}

	/** For the JWT filter: the user, only if they still exist and are not blocked. */
	@Transactional(readOnly = true)
	public Optional<User> findActiveById(Long id) {
		return users.findById(id).filter(User::isEnabled);
	}

	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return users.findByEmail(normalizeEmail(email));
	}

	@Transactional(readOnly = true)
	public User getById(Long id) {
		return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
	}

	/** e.g. SELLER when the admin approves a shop. Adding a role twice changes nothing. */
	@Transactional
	public void addRole(Long userId, Role role) {
		getById(userId).getRoles().add(role);
	}

	/** Emails are stored in lower case, so "Ravi@TriVoKo.test" and "ravi@trivoko.test" are one account. */
	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}

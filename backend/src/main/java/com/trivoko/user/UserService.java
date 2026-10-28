package com.trivoko.user;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.admin.AuditService;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.Likes;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.user.dto.AdminUserView;

import lombok.RequiredArgsConstructor;

/** The user module's counter: other modules (auth, seller, admin) come here, never to the repositories. */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final AuditService audit;

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

	// ---------- The admin's user page (Phase 6) ----------

	/** Newest accounts first. q = part of the email, name or phone. */
	@Transactional(readOnly = true)
	public PageResponse<AdminUserView> search(String q, Pageable pageable) {
		return PageResponse.from(users.search(Likes.contains(q), pageable), UserService::adminView);
	}

	@Transactional(readOnly = true)
	public long countUsers() {
		return users.count();
	}

	/** id -> email for a list that shows "who did it" (the activity log). Unknown ids are left out. */
	@Transactional(readOnly = true)
	public Map<Long, String> emailsOf(Collection<Long> ids) {
		return ids.isEmpty() ? Map.of()
				: users.findByIdIn(ids).stream().collect(Collectors.toMap(User::getId, User::getEmail));
	}

	/** id -> full name, e.g. for review authors. Unknown ids are left out. */
	@Transactional(readOnly = true)
	public Map<Long, String> namesOf(Collection<Long> ids) {
		return ids.isEmpty() ? Map.of()
				: users.findByIdIn(ids).stream().collect(Collectors.toMap(User::getId, User::getFullName));
	}

	/**
	 * Blocked = cannot log in, and the cookie they already have stops working on their next click (the JWT filter
	 * asks findActiveById on every request). An admin account cannot be blocked from the panel - so the admin can
	 * never lock themselves (or the only other admin) out.
	 */
	@Transactional
	public AdminUserView block(Long userId) {
		User user = getById(userId);
		if (user.getRoles().contains(Role.ADMIN)) {
			throw new BusinessRuleException("An admin account cannot be blocked.");
		}
		if (!user.isEnabled()) {
			throw new BusinessRuleException("This account is already blocked.");
		}
		user.setEnabled(false);
		audit.record("USER_BLOCKED", "USER", user.getId(), user.getEmail());
		return adminView(user);
	}

	@Transactional
	public AdminUserView unblock(Long userId) {
		User user = getById(userId);
		if (user.isEnabled()) {
			throw new BusinessRuleException("This account is not blocked.");
		}
		user.setEnabled(true);
		audit.record("USER_UNBLOCKED", "USER", user.getId(), user.getEmail());
		return adminView(user);
	}

	private static AdminUserView adminView(User u) {
		return new AdminUserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
				u.getRoles().stream().map(Enum::name).sorted().toList(), !u.isEnabled(), u.getCreatedAt());
	}

	/** Emails are stored in lower case, so "Ravi@TriVoKo.test" and "ravi@trivoko.test" are one account. */
	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}

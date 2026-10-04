package com.trivoko.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives the demo accounts from Flyway V3 (admin, Ravi, Kavya, the 9 shop owners) their password.
 *
 * Flyway cannot do this, because the password must never be in the code: V3 stores a placeholder
 * that can never match, and this runner replaces it with BCrypt(DEMO_PASSWORD). Runs only when
 * DEMO_PASSWORD is set: in .env on a laptop, or as a secret on a demo server. Empty = demo accounts
 * stay locked. Accounts whose password was already set are left alone. (Idea copied from EventHub.)
 */
@Component
public class DevDataSeeder implements ApplicationRunner {

	/** The password hash written by V3__accounts_and_roles.sql. */
	static final String PLACEHOLDER = "{noop}!locked";

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

	private final String password;

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	public DevDataSeeder(@Value("${app.demo.password:}") String password, UserRepository users,
			PasswordEncoder passwordEncoder) {
		this.password = password;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (password.isBlank()) {
			return;
		}
		String hash = passwordEncoder.encode(password); // one hash for all: BCrypt is slow on purpose
		int updated = 0;
		for (User user : users.findAll()) {
			if (PLACEHOLDER.equals(user.getPasswordHash())) {
				user.setPasswordHash(hash);
				updated++;
			}
		}
		if (updated > 0) {
			log.info("Demo accounts ready: {} (*@trivoko.test, password = DEMO_PASSWORD in .env)", updated);
		}
	}

}

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
 * Gives the demo accounts from Flyway V3 (admin, Ravi, Kavya, the 9 shop owners - all @trivoko.test)
 * their password.
 *
 * Flyway cannot do this, because the password must never be in the code: V3 stores a placeholder
 * that can never match, and this runner sets BCrypt(DEMO_PASSWORD) instead. It always follows the
 * CURRENT DEMO_PASSWORD: change it in .env, restart, and the demo logins use the new one.
 * Runs only when DEMO_PASSWORD is set (in .env on a laptop, or as a secret on a demo server);
 * empty = nothing changes. Real accounts (not @trivoko.test) are never touched. (Idea from EventHub.)
 */
@Component
public class DevDataSeeder implements ApplicationRunner {

	static final String DEMO_DOMAIN = "@trivoko.test";

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
		String hash = null; // made once, only if needed: BCrypt is slow on purpose
		int updated = 0;
		for (User user : users.findAll()) {
			boolean demo = user.getEmail().endsWith(DEMO_DOMAIN);
			// matches() is false for the V3 placeholder and for an older DEMO_PASSWORD
			if (demo && !passwordEncoder.matches(password, user.getPasswordHash())) {
				hash = hash == null ? passwordEncoder.encode(password) : hash;
				user.setPasswordHash(hash);
				updated++;
			}
		}
		if (updated > 0) {
			log.info("Demo accounts ready: {} (*{}, password = DEMO_PASSWORD)", updated, DEMO_DOMAIN);
		}
	}

}

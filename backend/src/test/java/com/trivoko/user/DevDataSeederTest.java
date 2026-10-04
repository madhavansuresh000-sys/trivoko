package com.trivoko.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

/**
 * The demo accounts always use the CURRENT DEMO_PASSWORD: change it in .env, restart, and the demo
 * logins follow. Real accounts (not @trivoko.test) are never touched.
 */
@SpringBootTest
@Transactional
class DevDataSeederTest {

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void demoAccountWithAnOldPasswordGetsTheCurrentOne() {
		User ravi = users.findByEmail("ravi@trivoko.test").orElseThrow();
		ravi.setPasswordHash(passwordEncoder.encode("an-old-demo-password"));

		new DevDataSeeder(Logins.PASSWORD, users, passwordEncoder).run(null);

		assertThat(passwordEncoder.matches(Logins.PASSWORD, ravi.getPasswordHash())).isTrue();
	}

	@Test
	void realAccountsAreNeverTouched() {
		User real = users.save(new User("someone@gmail.com", passwordEncoder.encode("their-own-password"), "Some One", null));

		new DevDataSeeder(Logins.PASSWORD, users, passwordEncoder).run(null);

		assertThat(passwordEncoder.matches("their-own-password", real.getPasswordHash())).isTrue();
	}

	@Test
	void emptyPasswordChangesNothing() {
		User kavya = users.findByEmail("kavya@trivoko.test").orElseThrow();
		String before = kavya.getPasswordHash();

		new DevDataSeeder("", users, passwordEncoder).run(null);

		assertThat(kavya.getPasswordHash()).isEqualTo(before);
	}

}

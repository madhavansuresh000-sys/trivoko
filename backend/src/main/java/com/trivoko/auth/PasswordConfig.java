package com.trivoko.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** How passwords are stored and checked. (Copied from EventHub.) */
@Configuration
public class PasswordConfig {

	/** BCrypt: slow on purpose (about 0.1 s per check), so guessing millions of passwords takes too long. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Checks email + password for AuthController.login: loads the user, compares with the BCrypt hash. */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

}

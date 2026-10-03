package com.trivoko.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.trivoko.common.SecurityProblems;

/**
 * Who may call which URL.
 *
 * Phase 0: only the health check and Swagger are open; every other URL answers 401.
 * Phase 1 opens the public catalogue (GET products, categories, sellers).
 * Phase 2 adds login (JWT cookie + CSRF, copied from EventHub) and locks Swagger to ADMIN.
 */
@Configuration
@EnableMethodSecurity // turns on @PreAuthorize on controller methods (used from Phase 2)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// no server-side session: from Phase 2 every request carries its own JWT cookie
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// same CSRF setup as EventHub (XSRF-TOKEN cookie -> X-XSRF-TOKEN header)
			.csrf(csrf -> csrf.spa())
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/health", "/actuator/health").permitAll()
				// TEMPORARY until Phase 2: the API map is open while there are no logins yet
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(SecurityProblems.notLoggedIn())
				.accessDeniedHandler(SecurityProblems.forbidden()));
		return http.build();
	}

}

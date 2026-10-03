package com.trivoko.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.trivoko.common.SecurityProblems;

/**
 * Who may call which URL.
 *
 * Open to everyone: the health check, Swagger (until Phase 2) and BROWSING the catalogue
 * (GET products, categories, sellers). Every other URL answers 401.
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
				// Phase 1: the shop window is public - anyone may BROWSE (GET only)
				.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/sellers/**").permitAll()
				// TEMPORARY until Phase 2: the API map is open while there are no logins yet
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(SecurityProblems.notLoggedIn())
				.accessDeniedHandler(SecurityProblems.forbidden()));
		return http.build();
	}

}

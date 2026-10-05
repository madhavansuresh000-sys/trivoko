package com.trivoko.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import com.trivoko.auth.CsrfCookieFilter;
import com.trivoko.auth.JwtCookieFilter;
import com.trivoko.auth.JwtService;
import com.trivoko.common.SecurityProblems;
import com.trivoko.user.UserService;

/**
 * Who may call which URL (Phase 2, spec section 4).
 *
 *   everyone   : health, BROWSING the catalogue (GET products, categories, sellers), register, login, csrf
 *   logged in  : addresses, "become a seller", everything not listed here
 *   SELLER     : /api/seller/** (and SellerAccess also checks the shop is APPROVED, so a block works at once)
 *   ADMIN      : /api/admin/**, Swagger (the API map)
 *
 * Like the mall gate: JwtCookieFilter reads your ID card, these rules decide which doors it opens.
 */
@Configuration
@EnableMethodSecurity // turns on @PreAuthorize on controller methods (SellerAccess checks)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, UserService userService)
			throws Exception {
		http
			// no server-side session: every request carries its own JWT cookie
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// CSRF for a single-page app (same as EventHub): the server sets an XSRF-TOKEN cookie that
			// JavaScript CAN read; Axios copies it into the X-XSRF-TOKEN header. Another website cannot
			// read our cookie, so its forged POST has no valid header and gets 403.
			// sessionAuthenticationStrategy: without server sessions Spring would think every request with
			// our JWT cookie is a new login and change the token each time; keep one token instead.
			.csrf(csrf -> csrf.spa().sessionAuthenticationStrategy((authentication, request, response) -> { }))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.cors(Customizer.withDefaults())
			.addFilterBefore(new JwtCookieFilter(jwtService, userService), UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/health", "/actuator/health").permitAll()
				.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/csrf").permitAll()
				// the shop window is public - anyone may BROWSE (GET only)
				.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/sellers/**").permitAll()
				// any logged-in customer may apply for a shop and see their application
				.requestMatchers("/api/seller/apply", "/api/seller/application").authenticated()
				.requestMatchers("/api/seller/**").hasRole("SELLER")
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				// the API map (Swagger) tells an attacker every URL, so only the admin may open it. The admin's
				// login cookie also works on :8080 (cookies are per host name, not per port): log in first.
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")
				.anyRequest().authenticated())
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(SecurityProblems.notLoggedIn())
				.accessDeniedHandler(SecurityProblems.forbidden()));
		return http.build();
	}

}

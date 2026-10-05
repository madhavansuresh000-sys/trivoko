package com.trivoko.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.trivoko.user.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs once for EVERY request, before the security rules:
 *
 * <pre>
 *   cookie -> JWT valid? -> user id 12 -> database: still enabled? current roles? -> "user 12, CUSTOMER + SELLER"
 * </pre>
 *
 * The JWT proves WHO you are; the database says what you may do NOW (spec decision P2-8). So when the
 * admin approves a shop, its owner can sell at once, and a blocked user is stopped at once.
 * No cookie, a fake or expired token, or a disabled user -> the request continues as a visitor,
 * and SecurityConfig answers 401 where login is needed.
 * Created in SecurityConfig (not a @Component, so it is not also registered as a plain servlet filter).
 */
public class JwtCookieFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	private final UserService userService;

	public JwtCookieFilter(JwtService jwtService, UserService userService) {
		this.jwtService = jwtService;
		this.userService = userService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Cookie cookie = WebUtils.getCookie(request, AuthCookies.NAME);
		if (cookie != null && !cookie.getValue().isBlank()) {
			jwtService.read(cookie.getValue())
				.flatMap(jwt -> userService.findActiveById(Long.valueOf(jwt.getSubject())))
				.ifPresent(user -> {
					AuthUser authUser = new AuthUser(user.getId(), user.getEmail(), user.getFullName(), user.getRoles());
					List<SimpleGrantedAuthority> authorities = authUser.roles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
						.toList();
					SecurityContext context = SecurityContextHolder.createEmptyContext();
					context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(authUser, null, authorities));
					SecurityContextHolder.setContext(context);
				});
		}
		chain.doFilter(request, response);
	}

}

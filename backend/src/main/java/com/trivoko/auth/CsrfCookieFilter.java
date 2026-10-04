package com.trivoko.auth;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring creates the CSRF token lazily (only when something asks for it), so the XSRF-TOKEN cookie
 * would be missing on the first page load and the React app's first POST (e.g. login) would fail with
 * 403. Asking for the token here makes the cookie appear on every response. (Copied from EventHub.)
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (token != null) {
			token.getToken(); // loading it writes the cookie
		}
		chain.doFilter(request, response);
	}

}

package com.trivoko.auth;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protects the live demo (after launch, decision D13). The one-click accounts are SHARED by every visitor, so
 * they may shop, pay (test page), pack, ship and return - but may not write text that other visitors then see:
 * reviews, product names and descriptions, product questions and answers. Like the demo phone in a shop: you may
 * try every app, but you cannot change its wallpaper for the next customer.
 * Only while one-click demo logins are on; a normal account (and Ravi on a laptop) is never limited.
 */
@Configuration
public class DemoGuard implements HandlerInterceptor, WebMvcConfigurer {

	static final String MESSAGE = "Demo accounts are shared by every visitor, so they cannot change what others see. "
			+ "Create a free account to try this.";

	/** Writes (not GET) to these URLs change text that every visitor sees. */
	private static final List<String> PUBLIC_TEXT = List.of(
			"/api/products/*/reviews/**",
			"/api/seller/products", "/api/seller/products/**",
			"/api/products/*/questions", "/api/products/*/questions/**",
			"/api/seller/questions/**");

	private static final AntPathMatcher PATHS = new AntPathMatcher();

	private final DemoAccounts demoAccounts;

	public DemoGuard(DemoAccounts demoAccounts) {
		this.demoAccounts = demoAccounts;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this).addPathPatterns("/api/**");
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!demoAccounts.enabled() || HttpMethod.GET.matches(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		boolean publicText = PUBLIC_TEXT.stream().anyMatch(p -> PATHS.match(p, path));
		if (publicText && AuthUser.current().map(u -> demoAccounts.isShared(u.email())).orElse(false)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, MESSAGE);
		}
		return true;
	}

}

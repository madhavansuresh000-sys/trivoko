package com.trivoko.auth;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.DemoAccounts.DemoAccount;
import com.trivoko.auth.dto.MeResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.user.User;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Phase 12 step 4: "Try it as Ravi" buttons on the login page, so a recruiter can look around without a password.
 *
 * Like a shop's demo phone on the counter: anyone may pick it up, but it is chained to the table - only the
 * DemoAccounts (never the admin), and DemoGuard stops them changing what other visitors see. Switched on with
 * app.demo.one-click=true (render.yaml), and only while the demo accounts are unlocked (DEMO_PASSWORD set).
 * Switched off = the list is empty and the login URL answers 404, as if it did not exist. See decision D13.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DemoLoginController {

	private final DemoAccounts demoAccounts;

	private final UserService userService;

	private final AuthService authService;

	private final JwtService jwtService;

	private final AuthCookies cookies;

	/** The buttons to show; empty when one-click demo login is off. */
	@GetMapping("/demo-accounts")
	public List<DemoAccount> accounts() {
		return demoAccounts.list();
	}

	/** Logs in as that demo account: the same httpOnly cookie as a normal login. */
	@PostMapping("/demo-login/{key}")
	public ResponseEntity<MeResponse> login(@PathVariable String key) {
		DemoAccount account = demoAccounts.byKey(key)
			.orElseThrow(() -> new ResourceNotFoundException("No demo login here."));
		// a demo account the admin blocked stays blocked
		User user = userService.findByEmail(account.email()).filter(User::isEnabled)
			.orElseThrow(() -> new ResourceNotFoundException("This demo account is not available."));
		AuthUser authUser = authService.forToken(user.getEmail());
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookies.login(jwtService.issue(authUser), jwtService.expiry()).toString())
			.body(authService.me(authUser));
	}

}

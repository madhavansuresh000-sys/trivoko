package com.trivoko.auth;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.dto.MeResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.user.User;
import com.trivoko.user.UserService;

/**
 * Phase 12 step 4: "Try it as Ravi" buttons on the login page, so a recruiter can look around without a password.
 *
 * Like a shop's demo phone on the counter: anyone may pick it up, but it is chained to the table - only these
 * accounts, never the admin (an admin could block every shop on the live demo). Switched on with
 * app.demo.one-click=true (render.yaml), and only while the demo accounts are unlocked (DEMO_PASSWORD set).
 * Switched off = the list is empty and the login URL answers 404, as if it did not exist. See decision D13.
 */
@RestController
@RequestMapping("/api/auth")
public class DemoLoginController {

	/** key in the URL -> demo account (ORDER below = button order). */
	static final Map<String, DemoAccount> ACCOUNTS = Map.of(
			"customer", new DemoAccount("customer", "ravi@trivoko.test", "Ravi", "Customer - shop, pay, track, return"),
			"seller", new DemoAccount("seller", "chennai.mobiles@trivoko.test", "Chennai Mobiles",
					"Seller - pack, ship, earnings"));

	private static final List<String> ORDER = List.of("customer", "seller");

	private final boolean enabled;

	private final UserService userService;

	private final AuthService authService;

	private final JwtService jwtService;

	private final AuthCookies cookies;

	public DemoLoginController(@Value("${app.demo.one-click:false}") boolean oneClick,
			@Value("${app.demo.password:}") String demoPassword, UserService userService, AuthService authService,
			JwtService jwtService, AuthCookies cookies) {
		// no DEMO_PASSWORD = the demo accounts are locked (DevDataSeeder), so no back door around that either
		this.enabled = oneClick && !demoPassword.isBlank();
		this.userService = userService;
		this.authService = authService;
		this.jwtService = jwtService;
		this.cookies = cookies;
	}

	/** The buttons to show; empty when one-click demo login is off. */
	@GetMapping("/demo-accounts")
	public List<DemoAccount> accounts() {
		return enabled ? ORDER.stream().map(ACCOUNTS::get).toList() : List.of();
	}

	/** Logs in as that demo account: the same httpOnly cookie as a normal login. */
	@PostMapping("/demo-login/{key}")
	public ResponseEntity<MeResponse> login(@PathVariable String key) {
		DemoAccount account = enabled ? ACCOUNTS.get(key) : null;
		if (account == null) {
			throw new ResourceNotFoundException("No demo login here.");
		}
		// a demo account the admin blocked stays blocked
		User user = userService.findByEmail(account.email()).filter(User::isEnabled)
			.orElseThrow(() -> new ResourceNotFoundException("This demo account is not available."));
		AuthUser authUser = authService.forToken(user.getEmail());
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookies.login(jwtService.issue(authUser), jwtService.expiry()).toString())
			.body(authService.me(authUser));
	}

	/** One button: key for the URL, who it is, and one line on what to try. */
	public record DemoAccount(String key, String email, String name, String tryThis) {
	}

}

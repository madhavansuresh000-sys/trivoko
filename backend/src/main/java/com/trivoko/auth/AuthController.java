package com.trivoko.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.dto.LoginRequest;
import com.trivoko.auth.dto.MeResponse;
import com.trivoko.auth.dto.RegisterRequest;
import com.trivoko.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Register, login, logout, "who am I" and the CSRF cookie (copied from EventHub).
 * The token never appears in the JSON: it travels only in the httpOnly cookie.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final AuthenticationManager authenticationManager;

	private final JwtService jwtService;

	private final AuthCookies cookies;

	private final LoginAttemptService loginAttempts;

	/** Creates a CUSTOMER account and logs it in straight away. 201 + cookie. */
	@PostMapping("/register")
	public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest request) {
		return withLoginCookie(HttpStatus.CREATED, authService.register(request));
	}

	/**
	 * Spring Security checks the password (BCrypt). Wrong email or password -> 401 with the SAME
	 * message for both, so nobody can find out which emails have accounts.
	 * 5 wrong passwords in a row -> locked for 15 minutes (429), see LoginAttemptService.
	 */
	@PostMapping("/login")
	public ResponseEntity<MeResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		String email = UserService.normalizeEmail(request.email());
		String address = http.getRemoteAddr();
		loginAttempts.checkAllowed(email, address);
		try {
			authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
		}
		catch (BadCredentialsException ex) {
			loginAttempts.failed(email, address);
			throw ex;
		}
		loginAttempts.succeeded(email, address);
		return withLoginCookie(HttpStatus.OK, authService.forToken(email));
	}

	/** Deletes the cookie. 204 No Content. */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.logout().toString()).build();
	}

	/** The logged-in user (guests get 401 from SecurityConfig before reaching this). */
	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AuthUser user) {
		return authService.me(user);
	}

	/** The React app calls this once on start: the response carries the XSRF-TOKEN cookie. */
	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf(CsrfToken token) {
		token.getToken();
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<MeResponse> withLoginCookie(HttpStatus status, AuthUser user) {
		String token = jwtService.issue(user);
		return ResponseEntity.status(status)
			.header(HttpHeaders.SET_COOKIE, cookies.login(token, jwtService.expiry()).toString())
			.body(authService.me(user));
	}

}

package com.trivoko.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The login cookie (copied from EventHub). httpOnly = JavaScript cannot read it, so a script injected
 * into a page cannot steal the token. SameSite=Lax = other websites cannot send it along with their
 * forms. Secure (HTTPS only) is switched on in prod.
 */
@Component
public class AuthCookies {

	public static final String NAME = "TRIVOKO_TOKEN";

	private final boolean secure;

	public AuthCookies(@Value("${app.cookie.secure}") boolean secure) {
		this.secure = secure;
	}

	public ResponseCookie login(String token, Duration maxAge) {
		return base(token).maxAge(maxAge).build();
	}

	/** Same cookie, empty and already expired: the browser deletes it. */
	public ResponseCookie logout() {
		return base("").maxAge(0).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Lax").path("/");
	}

}

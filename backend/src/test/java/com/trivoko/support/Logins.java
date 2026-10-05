package com.trivoko.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

/**
 * Logs in through the REAL login URL and returns the TRIVOKO_TOKEN cookie, so tests use the same
 * cookie path as the browser. The seed accounts get this password from MySqlTestDatabase
 * (app.demo.password) via DevDataSeeder.
 */
public final class Logins {

	public static final String PASSWORD = "Test-Demo-Pass-1";

	public static final String COOKIE = "TRIVOKO_TOKEN";

	private Logins() {
	}

	/** e.g. Logins.as(mvc, "ravi") -> logs in ravi@trivoko.test. A full email also works. */
	public static Cookie as(MockMvc mvc, String who) throws Exception {
		String email = who.contains("@") ? who : who + "@trivoko.test";
		return mvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getCookie(COOKIE);
	}

}

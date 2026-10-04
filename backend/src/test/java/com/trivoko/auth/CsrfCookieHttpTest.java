package com.trivoko.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * GET /api/auth/csrf gives the browser its XSRF-TOKEN cookie (readable by JavaScript, not httpOnly).
 * Checked against a REAL server on a random port: MockMvc's csrf() test helper replaces the CSRF
 * store for the rest of the test run, so a MockMvc check here could pass or fail by test order.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CsrfCookieHttpTest {

	@LocalServerPort
	private int port;

	@Test
	void csrfUrlGivesAReadableCookie() throws Exception {
		HttpResponse<Void> response = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/csrf")).build(),
				HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(204);
		String cookie = response.headers().allValues("Set-Cookie").stream()
			.filter(c -> c.startsWith("XSRF-TOKEN="))
			.findFirst().orElseThrow();
		assertThat(cookie).doesNotContainIgnoringCase("HttpOnly");
	}

}

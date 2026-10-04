package com.trivoko.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.trivoko.support.Logins;

/**
 * CSRF: a forged form on another website would carry our login cookie automatically, but it cannot
 * read our XSRF-TOKEN cookie, so it cannot send the matching X-XSRF-TOKEN header -> 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void postWithoutCsrfHeaderIsRefusedEvenWhenLoggedIn() throws Exception {
		mvc.perform(post("/api/auth/logout").cookie(Logins.as(mvc, "ravi")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value(Matchers.containsString("CSRF")));
	}

	@Test
	void loginWithoutCsrfHeaderIsRefused() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ravi@trivoko.test\",\"password\":\"" + Logins.PASSWORD + "\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void postWithCsrfHeaderWorks() throws Exception {
		mvc.perform(post("/api/auth/logout").with(csrf()).cookie(Logins.as(mvc, "ravi")))
			.andExpect(status().isNoContent());
	}

}

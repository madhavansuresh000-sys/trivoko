package com.trivoko.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

/** Phase 12 one-click demo logins: off by default, and when on only the customer and seller - never the admin. */
class DemoLoginTest {

	/** The normal settings (app.demo.one-click not set): the feature does not exist. */
	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@Transactional
	class SwitchedOff {

		@Autowired
		private MockMvc mvc;

		@Test
		void noButtonsAndNoLogin() throws Exception {
			mvc.perform(get("/api/auth/demo-accounts")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
			mvc.perform(post("/api/auth/demo-login/customer").with(csrf())).andExpect(status().isNotFound());
		}

	}

	@Nested
	@SpringBootTest(properties = "app.demo.one-click=true")
	@AutoConfigureMockMvc
	@Transactional
	class SwitchedOn {

		@Autowired
		private MockMvc mvc;

		@Autowired
		private JdbcTemplate jdbc;

		@Autowired
		private EntityManager entityManager;

		@Test
		void listsTheCustomerAndTheSellerButNoAdmin() throws Exception {
			mvc.perform(get("/api/auth/demo-accounts")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].key").value("customer"))
				.andExpect(jsonPath("$[0].email").value("ravi@trivoko.test"))
				.andExpect(jsonPath("$[1].key").value("seller"));
		}

		@Test
		void oneClickGivesTheSameCookieAsANormalLogin() throws Exception {
			Cookie cookie = mvc.perform(post("/api/auth/demo-login/seller").with(csrf()))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
				.andExpect(jsonPath("$.email").value("chennai.mobiles@trivoko.test"))
				.andReturn().getResponse().getCookie(Logins.COOKIE);
			assertThat(cookie).isNotNull();

			mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk())
				.andExpect(jsonPath("$.seller.slug").value("chennai-mobiles"));
		}

		@Test
		void theAdminAndUnknownKeysAreNotDemoLogins() throws Exception {
			mvc.perform(post("/api/auth/demo-login/admin").with(csrf())).andExpect(status().isNotFound());
			mvc.perform(post("/api/auth/demo-login/ravi@trivoko.test").with(csrf())).andExpect(status().isNotFound());
		}

		@Test
		void needsTheCsrfTokenLikeEveryPost() throws Exception {
			mvc.perform(post("/api/auth/demo-login/customer")).andExpect(status().isForbidden());
		}

		@Test
		void aBlockedDemoAccountStaysBlocked() throws Exception {
			jdbc.update("UPDATE users SET enabled = FALSE WHERE email = 'ravi@trivoko.test'");
			entityManager.clear();
			mvc.perform(post("/api/auth/demo-login/customer").with(csrf())).andExpect(status().isNotFound());
		}

	}

}

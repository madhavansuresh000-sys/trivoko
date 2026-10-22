package com.trivoko.admin;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** Phase 6: the admin finds a user and blocks them -> no login, and the cookie they have stops working at once. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUsersTest {

	private static final long RAVI = 2;

	private static final long ADMIN = 1;

	@Autowired
	private MockMvc mvc;

	private Cookie admin;

	@BeforeEach
	void login() throws Exception {
		admin = Logins.as(mvc, "admin");
	}

	@Test
	void searchFindsByNameOrEmail() throws Exception {
		mvc.perform(get("/api/admin/users").param("q", "kavya").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].email").value(contains("kavya@trivoko.test")))
			.andExpect(jsonPath("$.content[0].roles").value(contains("CUSTOMER")))
			.andExpect(jsonPath("$.content[0].blocked").value(false))
			.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
	}

	@Test
	void blockedUserIsLoggedOutAndCannotLogIn() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		mvc.perform(get("/api/auth/me").cookie(ravi)).andExpect(status().isOk());

		mvc.perform(post("/api/admin/users/" + RAVI + "/block").with(csrf()).cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.blocked").value(true));

		mvc.perform(get("/api/auth/me").cookie(ravi)).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"ravi@trivoko.test\",\"password\":\"" + Logins.PASSWORD + "\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("blocked")));

		mvc.perform(post("/api/admin/users/" + RAVI + "/unblock").with(csrf()).cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.blocked").value(false));
		Logins.as(mvc, "ravi"); // can log in again
	}

	@Test
	void anAdminCannotBeBlocked() throws Exception {
		mvc.perform(post("/api/admin/users/" + ADMIN + "/block").with(csrf()).cookie(admin))
			.andExpect(status().isConflict());
	}

	@Test
	void unblockingAnActiveUserIsRefused() throws Exception {
		mvc.perform(post("/api/admin/users/" + RAVI + "/unblock").with(csrf()).cookie(admin))
			.andExpect(status().isConflict());
	}

}

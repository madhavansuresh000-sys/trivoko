package com.trivoko.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

/** Register, login, "who am I" and logout through the real URLs and the httpOnly cookie. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager entityManager;

	private ResultActions register(String json) throws Exception {
		return mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	@Test
	void registerCreatesACustomerAndLogsIn() throws Exception {
		Cookie cookie = register("{\"email\":\" New@Shop.test \",\"password\":\"longenough1\",\"fullName\":\"New User\"}")
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
			.andExpect(jsonPath("$.email").value("new@shop.test"))
			.andReturn().getResponse().getCookie(Logins.COOKIE);
		assertThat(cookie).isNotNull();

		mvc.perform(get("/api/auth/me").cookie(cookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("new@shop.test"))
			.andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
			.andExpect(jsonPath("$.roles.length()").value(1))
			.andExpect(jsonPath("$.seller").doesNotExist());
	}

	@Test
	void sameEmailCannotRegisterTwice() throws Exception {
		register("{\"email\":\"RAVI@trivoko.test\",\"password\":\"longenough1\",\"fullName\":\"Other Ravi\"}")
			.andExpect(status().isConflict());
	}

	/** Review fix: DevDataSeeder resets every @trivoko.test password to DEMO_PASSWORD, so nobody may sign up with it. */
	@Test
	void demoDomainIsReservedForTheDemoAccounts() throws Exception {
		register("{\"email\":\" Eve@TriVoKo.test \",\"password\":\"longenough1\",\"fullName\":\"Eve Hacker\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("reserved")));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'eve@trivoko.test'", Integer.class))
			.isZero();
	}

	@Test
	void shortPasswordIsRejected() throws Exception {
		register("{\"email\":\"short@shop.test\",\"password\":\"short\",\"fullName\":\"Short Pass\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void wrongPasswordIs401WithTheSameMessageForEveryone() throws Exception {
		login("ravi@trivoko.test", "wrong-password-1").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Wrong email or password."));
		login("nobody@trivoko.test", "wrong-password-1").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Wrong email or password."));
	}

	/** Review focus: spaces and capital letters in the email still find the account. */
	@Test
	void emailIsNormalisedAtLogin() throws Exception {
		login(" Ravi@TriVoKo.test ", Logins.PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ravi@trivoko.test"));
	}

	@Test
	void sellerSeesTheirShopInMe() throws Exception {
		mvc.perform(get("/api/auth/me").cookie(Logins.as(mvc, "kovai.sports")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles.length()").value(2))
			.andExpect(jsonPath("$.seller.slug").value("kovai-sports"))
			.andExpect(jsonPath("$.seller.status").value("APPROVED"));
	}

	@Test
	void logoutDeletesTheCookie() throws Exception {
		mvc.perform(post("/api/auth/logout").with(csrf()).cookie(Logins.as(mvc, "ravi")))
			.andExpect(status().isNoContent())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
	}

	@Test
	void meWithoutLoginIs401() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	/** Review focus: blocking works at once, even though the old cookie has not expired. */
	@Test
	void disabledUsersOldCookieStopsWorking() throws Exception {
		Cookie cookie = Logins.as(mvc, "kavya");
		jdbc.update("UPDATE users SET enabled = FALSE WHERE email = 'kavya@trivoko.test'");
		// a real request starts with an empty Hibernate memory; this test transaction still remembers Kavya
		entityManager.clear();
		mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
	}

	@Test
	void fakeCookieIsIgnored() throws Exception {
		mvc.perform(get("/api/auth/me").cookie(new Cookie(Logins.COOKIE, "not-a-real-token")))
			.andExpect(status().isUnauthorized());
	}

}

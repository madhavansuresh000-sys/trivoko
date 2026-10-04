package com.trivoko.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.trivoko.support.Logins;

/** The door rules from spec section 4 that no other test covers. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

	@Autowired
	private MockMvc mvc;

	/** The API map tells an attacker every URL, so only the admin may open it. */
	@Test
	void swaggerIsForTheAdminOnly() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
		mvc.perform(get("/v3/api-docs").cookie(Logins.as(mvc, "ravi"))).andExpect(status().isForbidden());
		mvc.perform(get("/v3/api-docs").cookie(Logins.as(mvc, "admin"))).andExpect(status().isOk());
	}

	@Test
	void browsingTheShopNeedsNoLogin() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isOk());
		mvc.perform(get("/api/categories")).andExpect(status().isOk());
		mvc.perform(get("/api/sellers/chennai-mobiles")).andExpect(status().isOk());
	}

	/** Strangers are not told which URLs exist: everything unknown is simply "please log in". */
	@Test
	void unknownApiUrlIs401ForGuests() throws Exception {
		mvc.perform(get("/api/nope")).andExpect(status().isUnauthorized());
	}

	/** Spec section 8: a logged-in user gets a clear JSON 404 instead of a Whitelabel error page. */
	@Test
	void unknownApiUrlIsAJson404WhenLoggedIn() throws Exception {
		mvc.perform(get("/api/nope").cookie(Logins.as(mvc, "ravi")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("There is no API at /api/nope"));
	}

}

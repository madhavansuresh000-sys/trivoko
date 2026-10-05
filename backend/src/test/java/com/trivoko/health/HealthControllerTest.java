package com.trivoko.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.trivoko.auth.JwtService;
import com.trivoko.config.SecurityConfig;
import com.trivoko.user.UserService;

@WebMvcTest(HealthController.class)
// the security setup needs the JWT reader; the user lookup is never reached here (no cookie), so a mock is enough
@Import({ SecurityConfig.class, JwtService.class })
class HealthControllerTest {

	@MockitoBean
	private UserService userService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthIsPublicAndUp() throws Exception {
		mockMvc.perform(get("/api/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.app").value("trivoko"));
	}

	@Test
	void otherUrlsNeedLoginAndAnswerWithProblemJson() throws Exception {
		mockMvc.perform(get("/api/orders"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Please log in first."));
	}

}

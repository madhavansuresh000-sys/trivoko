package com.trivoko.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.trivoko.config.SecurityConfig;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

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

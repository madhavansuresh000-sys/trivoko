package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * POST /api/uploads/signature: the browser asks our server for a signature, then sends the photo
 * STRAIGHT to Cloudinary. The photo never passes through our server, and the API secret never
 * leaves it. Fake keys here; the real upload is CloudinaryLiveUploadTest.
 */
class UploadSignatureTest {

	private static final String URL = "/api/uploads/signature";

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@TestPropertySource(properties = {
			"app.cloudinary.cloud-name=trivoko-test",
			"app.cloudinary.api-key=111222333",
			"app.cloudinary.api-secret=test-secret-abc" })
	class WithKeys {

		@Autowired
		private MockMvc mvc;

		@Autowired
		private ObjectMapper json;

		@Test
		void notLoggedInIsRefused() throws Exception {
			mvc.perform(post(URL).with(csrf()))
				.andExpect(status().isUnauthorized());
		}

		@Test
		void withoutCsrfTokenIsRefused() throws Exception {
			mvc.perform(post(URL).with(user("kovai@trivoko.test")))
				.andExpect(status().isForbidden());
		}

		@Test
		void loggedInUserGetsAValidSignature() throws Exception {
			MvcResult result = mvc.perform(post(URL).with(user("kovai@trivoko.test")).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cloudName").value("trivoko-test"))
				.andExpect(jsonPath("$.apiKey").value("111222333"))
				.andExpect(jsonPath("$.folder").value("trivoko/products"))
				.andExpect(jsonPath("$.allowedFormats").value("jpg,jpeg,png,webp"))
				.andExpect(jsonPath("$.maxFileSize").value(5 * 1024 * 1024))
				.andExpect(jsonPath("$.uploadUrl").value("https://api.cloudinary.com/v1_1/trivoko-test/image/upload"))
				.andExpect(content().string(not(containsString("test-secret-abc"))))
				.andReturn();

			JsonNode body = json.readTree(result.getResponse().getContentAsString());
			long timestamp = body.get("timestamp").asLong();
			assertThat(timestamp).isCloseTo(System.currentTimeMillis() / 1000, org.assertj.core.data.Offset.offset(60L));

			// Cloudinary's rule: SHA-1 of the signed params sorted by name, joined with &, then the secret
			String toSign = "allowed_formats=jpg,jpeg,png,webp&folder=trivoko/products&timestamp=" + timestamp
					+ "test-secret-abc";
			String expected = HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-1").digest(toSign.getBytes(StandardCharsets.UTF_8)));
			assertThat(body.get("signature").asString()).isEqualTo(expected);
		}

	}

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@TestPropertySource(properties = {
			"app.cloudinary.cloud-name=",
			"app.cloudinary.api-key=",
			"app.cloudinary.api-secret=" })
	class WithoutKeys {

		@Autowired
		private MockMvc mvc;

		@Test
		void missingKeysGiveAClearMessage() throws Exception {
			mvc.perform(post(URL).with(user("kovai@trivoko.test")).with(csrf()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.detail").value("Photo upload is not set up yet (Cloudinary keys missing in .env)"));
		}

	}

}

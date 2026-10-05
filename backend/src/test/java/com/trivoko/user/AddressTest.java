package com.trivoko.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** /api/me/addresses: max 5, exactly one default, and nobody can touch another person's address. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AddressTest {

	private static final String URL = "/api/me/addresses";

	@Autowired
	private MockMvc mvc;

	private Cookie ravi;

	@BeforeEach
	void login() throws Exception {
		ravi = Logins.as(mvc, "ravi");
	}

	private static String body(String name, String pincode) {
		return """
				{"name":"%s","phone":"9876543210","line1":"12 Gandhi Road","line2":"","city":"Chennai",
				 "state":"Tamil Nadu","pincode":"%s"}""".formatted(name, pincode);
	}

	private ResultActions add(Cookie who, String name) throws Exception {
		return mvc.perform(post(URL).with(csrf()).cookie(who).contentType(MediaType.APPLICATION_JSON)
			.content(body(name, "600001")));
	}

	private long addId(Cookie who, String name) throws Exception {
		String json = add(who, name).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	@Test
	void firstAddressBecomesTheDefault() throws Exception {
		add(ravi, "Home").andExpect(status().isCreated()).andExpect(jsonPath("$.isDefault").value(true));
		add(ravi, "Office").andExpect(status().isCreated()).andExpect(jsonPath("$.isDefault").value(false));
	}

	@Test
	void sixthAddressIsRefused() throws Exception {
		for (int i = 1; i <= 5; i++) {
			add(ravi, "Place " + i).andExpect(status().isCreated());
		}
		add(ravi, "Place 6").andExpect(status().isConflict());
	}

	@Test
	void onlyOneDefault() throws Exception {
		addId(ravi, "One");
		addId(ravi, "Two");
		long three = addId(ravi, "Three");
		mvc.perform(put(URL + "/" + three + "/default").with(csrf()).cookie(ravi)).andExpect(status().isOk());
		mvc.perform(get(URL).cookie(ravi))
			.andExpect(jsonPath("$[?(@.isDefault == true)].name").value(org.hamcrest.Matchers.contains("Three")));
	}

	/** Review focus: deleting the default makes the newest remaining address the default. */
	@Test
	void deletingTheDefaultPicksTheNewestRemaining() throws Exception {
		long home = addId(ravi, "Home");     // default
		addId(ravi, "Office");
		addId(ravi, "Parents");              // newest
		mvc.perform(delete(URL + "/" + home).with(csrf()).cookie(ravi)).andExpect(status().isNoContent());
		mvc.perform(get(URL).cookie(ravi))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[?(@.isDefault == true)].name").value(org.hamcrest.Matchers.contains("Parents")));
	}

	@Test
	void editChangesTheAddress() throws Exception {
		long home = addId(ravi, "Home");
		mvc.perform(put(URL + "/" + home).with(csrf()).cookie(ravi).contentType(MediaType.APPLICATION_JSON)
				.content(body("New home", "641001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("New home"))
			.andExpect(jsonPath("$.pincode").value("641001"))
			.andExpect(jsonPath("$.isDefault").value(true));
	}

	@Test
	void someoneElsesAddressIsNotFound() throws Exception {
		long home = addId(ravi, "Home");
		Cookie kavya = Logins.as(mvc, "kavya");
		mvc.perform(put(URL + "/" + home).with(csrf()).cookie(kavya).contentType(MediaType.APPLICATION_JSON)
				.content(body("Hacked", "600001")))
			.andExpect(status().isNotFound());
		mvc.perform(delete(URL + "/" + home).with(csrf()).cookie(kavya)).andExpect(status().isNotFound());
		mvc.perform(put(URL + "/" + home + "/default").with(csrf()).cookie(kavya)).andExpect(status().isNotFound());
		mvc.perform(get(URL).cookie(kavya)).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void pincodeMustBeSixDigits() throws Exception {
		mvc.perform(post(URL).with(csrf()).cookie(ravi).contentType(MediaType.APPLICATION_JSON)
				.content(body("Home", "12345")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.pincode").exists());
	}

	@Test
	void guestsMustLogIn() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

}

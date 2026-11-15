package com.trivoko.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** Product questions: shoppers ask, only the product's shop answers, both get a bell. Volta V12 5G = Chennai Mobiles. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionTest {

	private static final String URL = "/api/products/volta-v12-5g/questions";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private ResultActions ask(Cookie who, String question) throws Exception {
		return mvc.perform(post(URL).with(csrf()).cookie(who).contentType(MediaType.APPLICATION_JSON)
			.content("{\"question\":\"" + question + "\"}"));
	}

	private ResultActions answer(Cookie seller, long id, String text) throws Exception {
		return mvc.perform(put("/api/seller/questions/" + id + "/answer").with(csrf()).cookie(seller)
			.contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"" + text + "\"}"));
	}

	private long askedId(Cookie who, String question) throws Exception {
		String json = ask(who, question).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private int bells(String email, String kind) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM notifications n JOIN users u ON u.id = n.user_id "
				+ "WHERE u.email = ? AND n.kind = ?", Integer.class, email, kind);
	}

	@Test
	void aShopperAsksEveryoneSeesItAndTheSellerGetsABell() throws Exception {
		int before = bells("chennai.mobiles@trivoko.test", "QUESTION_ASKED");
		ask(Logins.as(mvc, "ravi"), "Does it come with a charger in the box?")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.askedBy").value("Ravi K."))
			.andExpect(jsonPath("$.answer").doesNotExist());

		mvc.perform(get(URL)).andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].question").value("Does it come with a charger in the box?"));
		assertThat(bells("chennai.mobiles@trivoko.test", "QUESTION_ASKED")).isEqualTo(before + 1);
	}

	@Test
	void theSellerAnswersAndTheShopperGetsABell() throws Exception {
		long id = askedId(Logins.as(mvc, "kavya"), "Is the 256 GB model dual SIM?");
		Cookie shop = Logins.as(mvc, "chennai.mobiles");

		mvc.perform(get("/api/seller/questions").cookie(shop)).andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.id == " + id + ")].productSlug").value("volta-v12-5g"));
		answer(shop, id, "Yes, both models take two nano SIMs.").andExpect(status().isOk())
			.andExpect(jsonPath("$.answer").value("Yes, both models take two nano SIMs."));
		// correcting the answer does not ring the bell again
		answer(shop, id, "Yes - two nano SIMs, or one SIM and an eSIM.").andExpect(status().isOk());

		assertThat(bells("kavya@trivoko.test", "QUESTION_ANSWERED")).isEqualTo(1);
		mvc.perform(get("/api/seller/questions").param("answered", "true").cookie(shop))
			.andExpect(jsonPath("$.content[?(@.id == " + id + ")].answer").value("Yes - two nano SIMs, or one SIM and an eSIM."));
	}

	@Test
	void anotherShopCannotAnswer() throws Exception {
		long id = askedId(Logins.as(mvc, "ravi"), "How long is the warranty?");
		answer(Logins.as(mvc, "kovai.sports"), id, "Not ours to say").andExpect(status().isForbidden());
	}

	@Test
	void atMostThreeOpenQuestionsPerProduct() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		for (int i = 1; i <= 3; i++) {
			ask(ravi, "Question number " + i + " about this phone?").andExpect(status().isCreated());
		}
		ask(ravi, "Question number 4 about this phone?").andExpect(status().isConflict());
	}

	@Test
	void guestsReadButCannotAsk() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isOk());
		mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"question\":\"Is this phone waterproof?\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void tooShortQuestionAndUnknownProduct() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		ask(ravi, "Why?").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.question").exists());
		mvc.perform(post("/api/products/no-such-thing/questions").with(csrf()).cookie(ravi)
			.contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Is this in stock soon?\"}"))
			.andExpect(status().isNotFound());
	}

}

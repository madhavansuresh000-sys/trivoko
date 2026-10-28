package com.trivoko.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** Phase 10 step 4 - suggestions while typing: from the 2nd letter, at most 8, brands first, live products only. */
@SpringBootTest
@AutoConfigureMockMvc
class SuggestTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private String suggest(String typed) throws Exception {
		return mvc.perform(get("/api/search/suggest").param("q", typed)).andExpect(status().isOk()).andReturn()
			.getResponse().getContentAsString();
	}

	@Test
	void oneLetterIsTooLittleToGuess() throws Exception {
		assertThat((List<?>) JsonPath.read(suggest("i"), "$")).isEmpty();
		assertThat((List<?>) JsonPath.read(suggest(" "), "$")).isEmpty();
	}

	@Test
	void doneWhen_twoLettersAlreadySuggestTheIphoneCase() throws Exception {
		String json = suggest("ip");
		List<String> slugs = JsonPath.read(json, "$[?(@.type == 'product')].slug");
		assertThat(slugs).contains("silicone-case-for-iphone-15");
	}

	@Test
	void aBrandComesFirstThenItsProducts() throws Exception {
		String json = suggest("vol");
		assertThat((String) JsonPath.read(json, "$[0].type")).isEqualTo("brand");
		assertThat((String) JsonPath.read(json, "$[0].text")).isEqualTo("Volta");
		List<String> products = JsonPath.read(json, "$[?(@.type == 'product')].slug");
		assertThat(products).contains("volta-v12-5g");
	}

	@Test
	void severalWordsMustAllMatchAndAtMostEightComeBack() throws Exception {
		assertThat((List<String>) JsonPath.read(suggest("silicone ca"), "$[?(@.type == 'product')].slug"))
			.containsExactly("silicone-case-for-iphone-15");
		assertThat((List<?>) JsonPath.read(suggest("pu"), "$")).hasSizeLessThanOrEqualTo(8);
	}

	@Test
	void aBlockedShopIsNeverSuggested() throws Exception {
		long chennai = jdbc.queryForObject("SELECT seller_id FROM products WHERE slug = 'volta-v12-5g'", Long.class);
		jdbc.update("UPDATE sellers SET status = 'BLOCKED' WHERE id = ?", chennai);
		try {
			String json = suggest("volta");
			assertThat((List<String>) JsonPath.read(json, "$[*].slug")).doesNotContain("volta-v12-5g")
				// Volta is also sold by Bengaluru Gadget Hub, so the brand and that shop's product stay
				.contains("volta-64-gb-pen-drive");
			assertThat((List<String>) JsonPath.read(json, "$[?(@.type == 'brand')].text")).containsExactly("Volta");
		}
		finally {
			jdbc.update("UPDATE sellers SET status = 'APPROVED' WHERE id = ?", chennai);
		}
	}

}

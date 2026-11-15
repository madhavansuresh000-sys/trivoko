package com.trivoko.catalog;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** GET /api/products/cards?ids=... for "Recently viewed": fresh cards, the browser's order, only products on sale. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductCardsTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long idOf(String slug) {
		return jdbc.queryForObject("SELECT id FROM products WHERE slug = ?", Long.class, slug);
	}

	@Test
	void cardsComeInTheAskedOrderWithoutProductsThatAreNotOnSale() throws Exception {
		long volta = idOf("volta-v12-5g");
		long kaveri = idOf("kaveri-k5");
		long notLive = idOf("arc-soundbar-2-1"); // in the seed but not on sale
		mvc.perform(get("/api/products/cards").param("ids", kaveri + "," + notLive + "," + volta + "," + kaveri + ",999999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].slug", contains("kaveri-k5", "volta-v12-5g")))
			.andExpect(jsonPath("$[0].priceFrom").value(8999.00));
	}

	@Test
	void atMostTwelveCards() throws Exception {
		String ids = String.join(",", jdbc.queryForList(
				"SELECT CAST(id AS CHAR) FROM products WHERE status = 'ACTIVE' ORDER BY id LIMIT 20", String.class));
		mvc.perform(get("/api/products/cards").param("ids", ids)).andExpect(jsonPath("$.length()").value(12));
	}

	@Test
	void anIdThatIsNotANumberIs400() throws Exception {
		mvc.perform(get("/api/products/cards").param("ids", "abc")).andExpect(status().isBadRequest());
	}

}

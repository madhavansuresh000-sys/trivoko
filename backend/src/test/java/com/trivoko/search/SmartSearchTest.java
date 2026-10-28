package com.trivoko.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Phase 10 step 3 - GET /api/search. Lucene finds and ranks the words (forgiving typos); MySQL applies the filters
 * and counts the facets, so stock and blocked shops are always fresh (decision D11).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SmartSearchTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private String search(String query) throws Exception {
		return mvc.perform(get("/api/search?" + query)).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();
	}

	private List<String> slugs(String json) {
		return JsonPath.read(json, "$.results.content[*].slug");
	}

	private long liveVolta() {
		return jdbc.queryForObject("""
				SELECT COUNT(*) FROM products p JOIN sellers s ON s.id = p.seller_id
				WHERE p.brand = 'Volta' AND p.status = 'ACTIVE' AND s.status = 'APPROVED'""", Long.class);
	}

	@Test
	void doneWhen_iphnoeFindsTheIphoneCaseFirst() throws Exception {
		String json = search("q=iphnoe");

		assertThat(slugs(json)).first().isEqualTo("silicone-case-for-iphone-15");
		// the D1 trap: "iphnoe" is 2 typos from "phones" - the phones of "Mobile phones" must NOT come along
		assertThat(slugs(json)).doesNotContain("volta-v12-5g");
	}

	@Test
	void twoWordsWithATypoStillFindIt() throws Exception {
		assertThat(slugs(search("q=iphnoe%20case"))).first().isEqualTo("silicone-case-for-iphone-15");
		assertThat(slugs(search("q=IPHONE"))).first().isEqualTo("silicone-case-for-iphone-15");
	}

	@Test
	void shortWordsMatchTheStartOfAWordButNotTypos() throws Exception {
		assertThat(slugs(search("q=silic"))).contains("silicone-case-for-iphone-15");
		assertThat(slugs(search("q=cqse"))).isEmpty(); // 4 letters: no typo allowed
	}

	@Test
	void aBrandFindsAllItsLiveProductsAndCountsThem() throws Exception {
		String json = search("q=volta");

		assertThat(((Number) JsonPath.read(json, "$.results.totalElements")).longValue()).isEqualTo(liveVolta());
		List<Integer> voltaCount = JsonPath.read(json, "$.facets.brands[?(@.value == 'Volta')].count");
		assertThat(voltaCount).containsExactly((int) liveVolta());
	}

	@Test
	void aBlockedShopNeverAppears() throws Exception {
		long chennai = jdbc.queryForObject("SELECT seller_id FROM products WHERE slug = 'volta-v12-5g'", Long.class);
		jdbc.update("UPDATE sellers SET status = 'BLOCKED' WHERE id = ?", chennai);
		try {
			assertThat(slugs(search("q=volta"))).doesNotContain("volta-v12-5g");
		}
		finally {
			jdbc.update("UPDATE sellers SET status = 'APPROVED' WHERE id = ?", chennai);
		}
	}

	@Test
	void inStockUsesTheStockOfRightNow() throws Exception {
		// a bulk UPDATE, like checkout and the flash sale do - Hibernate (and so Lucene) never sees it
		List<Map<String, Object>> before = jdbc.queryForList(
				"SELECT v.id, v.stock FROM product_variants v JOIN products p ON p.id = v.product_id WHERE p.slug = 'volta-v12-5g'");
		jdbc.update("UPDATE product_variants v JOIN products p ON p.id = v.product_id SET v.stock = 0 WHERE p.slug = 'volta-v12-5g'");
		try {
			assertThat(slugs(search("q=volta&inStock=true"))).doesNotContain("volta-v12-5g");
			assertThat(slugs(search("q=volta"))).contains("volta-v12-5g");
		}
		finally {
			before.forEach(row -> jdbc.update("UPDATE product_variants SET stock = ? WHERE id = ?", row.get("stock"),
					row.get("id")));
		}
	}

	@Test
	void tickingABrandNarrowsTheResultsButKeepsTheOtherBrandsCounted() throws Exception {
		String all = search("q=headphones"); // Arc and Pulse both make headphones
		List<String> brands = JsonPath.read(all, "$.facets.brands[*].value");
		assertThat(brands).hasSizeGreaterThan(1);
		String first = brands.getFirst();

		String one = search("q=headphones&brand=" + first);
		List<String> shownBrands = JsonPath.read(one, "$.results.content[*].brand");
		assertThat(shownBrands).isNotEmpty().allMatch(first::equals);
		// the brand list itself ignores the brand tick, so you can still tick a second brand
		assertThat((List<String>) JsonPath.read(one, "$.facets.brands[*].value")).containsExactlyElementsOf(brands);
	}

	@Test
	void categoryAndPriceCountsAddUpToTheResults() throws Exception {
		String json = search("q=volta");
		int total = JsonPath.read(json, "$.results.totalElements");
		List<Integer> perCategory = JsonPath.read(json, "$.facets.categories[*].count");
		List<Integer> perPrice = JsonPath.read(json, "$.facets.prices[*].count");
		assertThat(perCategory.stream().mapToInt(Integer::intValue).sum()).isEqualTo(total);
		assertThat(perPrice.stream().mapToInt(Integer::intValue).sum()).isEqualTo(total);
	}

	@Test
	void sortByPriceInsteadOfRelevance() throws Exception {
		List<Double> prices = JsonPath.read(search("q=volta&sort=price&dir=asc"), "$.results.content[*].priceFrom");
		assertThat(prices).isSorted();
	}

	@Test
	void aSearchNeedsWords() throws Exception {
		mvc.perform(get("/api/search").param("q", "  ")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/search?q=zzqqxx"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.results.totalElements").value(0));
	}

}

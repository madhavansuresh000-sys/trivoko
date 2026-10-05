package com.trivoko.catalog;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Phase 3: the simple ?q= search box and the brand list for the filter (seed data V2). */
@SpringBootTest
@AutoConfigureMockMvc
class ProductSearchTest {

	@Autowired
	private MockMvc mvc;

	/** Every word must match the name or the brand: "volta cover" = Volta products that are covers. */
	@Test
	void everyWordMustMatchNameOrBrand() throws Exception {
		mvc.perform(get("/api/products").param("q", "  VOLTA   cover ").param("size", "48"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name", hasItem("Leather flip cover for Volta V12")))
			.andExpect(jsonPath("$.content[*].name", everyItem(containsStringIgnoringCase("cover"))))
			.andExpect(jsonPath("$.content[*].name", everyItem(containsStringIgnoringCase("volta"))));
	}

	/** The brand alone is enough for a word: "kaveri" finds the Kaveri phone by its brand too. */
	@Test
	void aWordCanMatchTheBrand() throws Exception {
		mvc.perform(get("/api/products").param("q", "kaveri").param("category", "phones"))
			.andExpect(jsonPath("$.content[*].name", contains("Kaveri K5")));
	}

	/** Review focus: % and _ are plain letters here, not SQL wildcards ("100%" must not match everything). */
	@Test
	void percentAndUnderscoreAreNotWildcards() throws Exception {
		mvc.perform(get("/api/products").param("q", "100%"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/products").param("q", "_"))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void blankQueryMeansNoFilter() throws Exception {
		mvc.perform(get("/api/products").param("q", "   "))
			.andExpect(jsonPath("$.totalElements").value(116));
	}

	// ---------- GET /api/products/brands ----------

	@Test
	void brandsOfACategoryAreSortedWithCounts() throws Exception {
		mvc.perform(get("/api/products/brands").param("category", "phones"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].brand", contains("Kaveri", "Nimbus", "Orbit", "Volta")))
			.andExpect(jsonPath("$[*].count", everyItem(org.hamcrest.Matchers.is(1))));
	}

	/** A top category includes its sub-categories, and hidden products are not counted. */
	@Test
	void topCategoryBrandsIncludeSubCategories() throws Exception {
		mvc.perform(get("/api/products/brands").param("category", "mobiles-accessories"))
			.andExpect(jsonPath("$[*].brand", hasItem("Volta")))
			.andExpect(jsonPath("$[?(@.brand == 'Volta')].count", contains(4)));
	}

	@Test
	void unknownCategoryGivesNoBrands() throws Exception {
		mvc.perform(get("/api/products/brands").param("category", "no-such-thing"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void withoutACategoryAllVisibleBrandsAreListed() throws Exception {
		mvc.perform(get("/api/products/brands"))
			.andExpect(jsonPath("$[*].brand", hasItem("Volta")))
			.andExpect(jsonPath("$[*].brand", hasItem("Kovai Run")));
	}

}

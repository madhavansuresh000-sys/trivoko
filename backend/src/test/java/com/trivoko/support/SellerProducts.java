package com.trivoko.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Shared bits for the seller product tests: a valid product JSON and "create one, give me its id". */
public final class SellerProducts {

	private SellerProducts() {
	}

	/** Any sub-category id from the seed (a product must sit in a sub-category, not a top one). */
	public static long subCategoryId(JdbcTemplate jdbc) {
		return jdbc.queryForObject("SELECT MIN(id) FROM categories WHERE parent_id IS NOT NULL", Long.class);
	}

	public static String json(long categoryId, String name, String price, String mrp, int stock) {
		return """
				{"name":"%s","categoryId":%d,"brand":"Volta","description":"A test product made by a test",
				 "variants":[{"label":"Black / 128 GB","colour":"Black","price":%s,"mrp":%s,"stock":%d}]}"""
			.formatted(name, categoryId, price, mrp, stock);
	}

	public static long create(MockMvc mvc, Cookie seller, String json) throws Exception {
		String body = mvc.perform(post("/api/seller/products").with(csrf()).cookie(seller)
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

}

package com.trivoko.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;
import com.trivoko.support.SellerProducts;

import jakarta.servlet.http.Cookie;

/**
 * Phase 2 "done when": a seller cannot touch another seller's product (403).
 * Kovai Sports (seller 2) tries every product URL on a Chennai Mobiles (seller 1) product.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerOwnershipTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private Cookie kovai;

	private long chennaiProduct;

	@BeforeEach
	void setUp() throws Exception {
		kovai = Logins.as(mvc, "kovai.sports");
		chennaiProduct = jdbc.queryForObject("SELECT MIN(id) FROM products WHERE seller_id = 1", Long.class);
	}

	private Map<String, Object> row() {
		return jdbc.queryForMap("SELECT name, status, price_from FROM products WHERE id = ?", chennaiProduct);
	}

	@Test
	void cannotReadAnotherShopsProduct() throws Exception {
		mvc.perform(get("/api/seller/products/" + chennaiProduct).cookie(kovai))
			.andExpect(status().isForbidden());
	}

	@Test
	void cannotEditAnotherShopsProduct() throws Exception {
		Map<String, Object> before = row();
		String json = SellerProducts.json(SellerProducts.subCategoryId(jdbc), "Hacked name", "1.00", "2.00", 1);
		mvc.perform(put("/api/seller/products/" + chennaiProduct).with(csrf()).cookie(kovai)
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isForbidden());
		assertThat(row()).isEqualTo(before);
	}

	@Test
	void cannotSubmitAnotherShopsProduct() throws Exception {
		Map<String, Object> before = row();
		mvc.perform(post("/api/seller/products/" + chennaiProduct + "/submit").with(csrf()).cookie(kovai))
			.andExpect(status().isForbidden());
		assertThat(row()).isEqualTo(before);
	}

	@Test
	void myListHasOnlyMyProducts() throws Exception {
		int kovaiProducts = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE seller_id = 2", Integer.class);
		mvc.perform(get("/api/seller/products").param("size", "48").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(kovaiProducts))
			.andExpect(jsonPath("$.content[?(@.id == " + chennaiProduct + ")]").isEmpty());
	}

	@Test
	void unknownProductIsNotFound() throws Exception {
		mvc.perform(get("/api/seller/products/999999").cookie(kovai)).andExpect(status().isNotFound());
	}

	@Test
	void ownerCanReadTheirProductWithExactStock() throws Exception {
		mvc.perform(get("/api/seller/products/" + chennaiProduct).cookie(Logins.as(mvc, "chennai.mobiles")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.variants[0].stock").isNumber());
	}

}

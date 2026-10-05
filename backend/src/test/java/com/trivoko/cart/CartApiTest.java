package com.trivoko.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

/** The cart URLs with the real login cookie, against the seed data (V2-V4). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager entityManager;

	/** A variant with plenty of stock from Chennai Mobiles (seller 1) and one from Kovai Sports (seller 2). */
	private long chennai;

	private long kovai;

	@BeforeEach
	void pickVariants() {
		chennai = variantOf(1);
		kovai = variantOf(2);
	}

	private long variantOf(long sellerId) {
		return jdbc.queryForObject("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				WHERE p.seller_id = ? AND p.status = 'ACTIVE' AND v.stock >= 20 ORDER BY v.id LIMIT 1""",
				Long.class, sellerId);
	}

	private ResultActions setQty(Cookie who, long variantId, int quantity) throws Exception {
		return mvc.perform(put("/api/cart/items/" + variantId).cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}"));
	}

	private ResultActions post(String url, Cookie who, String json) throws Exception {
		var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(json);
		return mvc.perform(who == null ? request : request.cookie(who));
	}

	private int savedLines() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM cart_items", Integer.class);
	}

	@Test
	void guestPreviewNeedsNoLoginAndSavesNothing() throws Exception {
		post("/api/cart/preview", null, "{\"items\":[{\"variantId\":" + chennai + ",\"quantity\":1},"
				+ "{\"variantId\":" + kovai + ",\"quantity\":2},{\"variantId\":999999,\"quantity\":1}]}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.packages.length()").value(2))
			.andExpect(jsonPath("$.packages[*].seller.slug", contains("chennai-mobiles", "kovai-sports")))
			.andExpect(jsonPath("$.itemCount").value(3));
		assertThat(savedLines()).isZero();
	}

	@Test
	void myCartNeedsLogin() throws Exception {
		mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/cart/items/" + chennai).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":1}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void writesNeedTheCsrfToken() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		mvc.perform(put("/api/cart/items/" + chennai).cookie(ravi).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":1}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void putSetsTheQuantityAndTheCartIsSaved() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		setQty(ravi, chennai, 2).andExpect(status().isOk());
		setQty(ravi, chennai, 5).andExpect(jsonPath("$.itemCount").value(5));

		mvc.perform(get("/api/cart").cookie(ravi))
			.andExpect(jsonPath("$.packages[0].items[0].variantId").value(chennai))
			.andExpect(jsonPath("$.packages[0].items[0].quantity").value(5));
		assertThat(savedLines()).isEqualTo(1);
	}

	@Test
	void badQuantityOrUnknownVariantIsRefused() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		setQty(ravi, chennai, 0).andExpect(status().isBadRequest());
		setQty(ravi, chennai, 11).andExpect(status().isBadRequest());
		setQty(ravi, 999999, 1).andExpect(status().isNotFound());
	}

	@Test
	void soldOutItemCannotBeAdded() throws Exception {
		jdbc.update("UPDATE product_variants SET stock = 0 WHERE id = ?", chennai);
		entityManager.clear();
		setQty(Logins.as(mvc, "ravi"), chennai, 1)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("cannot be bought")));
	}

	@Test
	void deleteRemovesTheLineAndTwiceIsFine() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		setQty(ravi, chennai, 1);
		setQty(ravi, kovai, 1);
		mvc.perform(delete("/api/cart/items/" + chennai).cookie(ravi).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.packages[*].seller.slug", contains("kovai-sports")));
		mvc.perform(delete("/api/cart/items/" + chennai).cookie(ravi).with(csrf())).andExpect(status().isOk());
	}

	/** Phase 3 "done when": the guest's lines join the saved cart; the same variant adds up, capped at 10. */
	@Test
	void mergeAddsTheGuestCartAndCapsAtTen() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		setQty(ravi, chennai, 7);
		post("/api/cart/merge", ravi, "{\"items\":[{\"variantId\":" + chennai + ",\"quantity\":6},"
				+ "{\"variantId\":" + kovai + ",\"quantity\":1},{\"variantId\":999999,\"quantity\":1}]}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.packages.length()").value(2))
			.andExpect(jsonPath("$.packages[0].items[0].quantity").value(10))
			.andExpect(jsonPath("$.itemCount").value(11));
	}

	@Test
	void thirtyFirstLineIsRefused() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		var ids = jdbc.queryForList("""
				SELECT v.id FROM product_variants v JOIN products p ON p.id = v.product_id
				JOIN sellers s ON s.id = p.seller_id
				WHERE p.status = 'ACTIVE' AND s.status = 'APPROVED' AND v.stock > 0 ORDER BY v.id LIMIT 31""", Long.class);
		for (int i = 0; i < 30; i++) {
			setQty(ravi, ids.get(i), 1).andExpect(status().isOk());
		}
		setQty(ravi, ids.get(30), 1)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A cart can hold at most 30 different items."));
	}

	/** Review focus: the shop is blocked while its item sits in the cart. */
	@Test
	void blockedShopsItemStaysButIsNotCounted() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		setQty(ravi, chennai, 2);
		jdbc.update("UPDATE sellers SET status = 'BLOCKED' WHERE id = 1");
		entityManager.clear();
		mvc.perform(get("/api/cart").cookie(ravi))
			.andExpect(jsonPath("$.packages[0].items[0].available").value(false))
			.andExpect(jsonPath("$.packages[0].items[0].note").value("No longer available"))
			.andExpect(jsonPath("$.itemCount").value(0));
	}

	@Test
	void everyoneSeesOnlyTheirOwnCart() throws Exception {
		setQty(Logins.as(mvc, "ravi"), chennai, 3);
		mvc.perform(get("/api/cart").cookie(Logins.as(mvc, "kavya")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.packages.length()").value(0))
			.andExpect(jsonPath("$.itemCount").value(0));
	}

}

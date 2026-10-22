package com.trivoko.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 6 "done when": the admin blocks a seller -> their products disappear from the shop (listing, product
 * page, search), and come back after unblock. The owner is told why.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerBlockTest {

	private static final long CHENNAI_MOBILES = 1;

	@Autowired
	private MockMvc mvc;

	private Cookie admin;

	@BeforeEach
	void login() throws Exception {
		admin = Logins.as(mvc, "admin");
	}

	private ResultActions block(long sellerId, String reason) throws Exception {
		return mvc.perform(post("/api/admin/sellers/" + sellerId + "/block").with(csrf()).cookie(admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"));
	}

	private ResultActions unblock(long sellerId) throws Exception {
		return mvc.perform(post("/api/admin/sellers/" + sellerId + "/unblock").with(csrf()).cookie(admin));
	}

	@Test
	void blockedShopsProductsDisappearAndComeBack() throws Exception {
		mvc.perform(get("/api/products/volta-v12-5g")).andExpect(status().isOk());

		block(CHENNAI_MOBILES, "Selling fake chargers")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("BLOCKED"))
			.andExpect(jsonPath("$.blockReason").value("Selling fake chargers"));

		mvc.perform(get("/api/products/volta-v12-5g")).andExpect(status().isNotFound());
		mvc.perform(get("/api/products").param("q", "volta v12")).andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/products").param("seller", "chennai-mobiles")).andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/sellers/chennai-mobiles")).andExpect(status().isNotFound());

		unblock(CHENNAI_MOBILES)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.blockReason").doesNotExist());
		mvc.perform(get("/api/products/volta-v12-5g")).andExpect(status().isOk());
	}

	@Test
	void blockNeedsAReason() throws Exception {
		mvc.perform(post("/api/admin/sellers/1/block").with(csrf()).cookie(admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void onlyABlockedShopCanBeUnblocked() throws Exception {
		unblock(CHENNAI_MOBILES).andExpect(status().isConflict());
	}

	@Test
	void theOwnerIsToldWhy() throws Exception {
		block(CHENNAI_MOBILES, "Selling fake chargers").andExpect(status().isOk());
		Cookie owner = Logins.as(mvc, "chennai.mobiles");
		mvc.perform(get("/api/notifications").cookie(owner))
			.andExpect(jsonPath("$.items[0].kind").value("SHOP_BLOCKED"))
			.andExpect(jsonPath("$.items[0].body").value(org.hamcrest.Matchers.containsString("Selling fake chargers")));
	}

	@Test
	void approvingAShopTellsTheOwner() throws Exception {
		mvc.perform(post("/api/admin/sellers/9/approve").with(csrf()).cookie(admin)).andExpect(status().isOk());
		Cookie owner = Logins.as(mvc, "erode.organics");
		mvc.perform(get("/api/notifications").cookie(owner))
			.andExpect(jsonPath("$.items[0].kind").value("SHOP_APPROVED"));
	}

	@Test
	void theBlockIsInTheActivityLog() throws Exception {
		block(CHENNAI_MOBILES, "Selling fake chargers").andExpect(status().isOk());
		unblock(CHENNAI_MOBILES).andExpect(status().isOk());
		mvc.perform(get("/api/admin/activity").cookie(admin))
			.andExpect(jsonPath("$.content[0].action").value("SELLER_UNBLOCKED"))
			.andExpect(jsonPath("$.content[0].actorEmail").value("admin@trivoko.test"))
			.andExpect(jsonPath("$.content[1].action").value("SELLER_BLOCKED"));
	}

}

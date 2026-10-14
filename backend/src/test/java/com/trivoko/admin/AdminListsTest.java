package com.trivoko.admin;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** Phase 6: the admin's shop and product lists are paged and have a search box. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminListsTest {

	@Autowired
	private MockMvc mvc;

	private Cookie admin;

	@BeforeEach
	void login() throws Exception {
		admin = Logins.as(mvc, "admin");
	}

	@Test
	void shopsArePagedAndSearchable() throws Exception {
		mvc.perform(get("/api/admin/sellers").param("status", "APPROVED").param("q", "chennai").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].slug").value(contains("chennai-mobiles")))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void noStatusMeansEveryShop() throws Exception {
		mvc.perform(get("/api/admin/sellers").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(9)))
			.andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void searchFindsACity() throws Exception {
		mvc.perform(get("/api/admin/sellers").param("q", "Coimbatore").cookie(admin))
			.andExpect(jsonPath("$.content[*].slug").value(contains("kovai-sports")));
	}

	@Test
	void productsArePagedAndSearchableWithTheShopName() throws Exception {
		mvc.perform(get("/api/admin/products").param("status", "ACTIVE").param("q", "volta v12").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].slug").value(contains("volta-v12-5g")))
			.andExpect(jsonPath("$.content[0].shopName").value("Chennai Mobiles"))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

}

package com.trivoko.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;
import com.trivoko.support.SellerProducts;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

/** DRAFT -> PENDING -> (admin) ACTIVE and visible to everyone, or REJECTED with a reason -> fixed -> again. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductLifecycleTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	@Autowired
	private AuditService audit;

	private Cookie kovai;

	private Cookie admin;

	private long subCategory;

	@BeforeEach
	void setUp() throws Exception {
		kovai = Logins.as(mvc, "kovai.sports");
		admin = Logins.as(mvc, "admin");
		subCategory = SellerProducts.subCategoryId(jdbc);
	}

	private long submitted(String name) throws Exception {
		long id = SellerProducts.create(mvc, kovai, SellerProducts.json(subCategory, name, "999.00", "1299.00", 7));
		mvc.perform(post("/api/seller/products/" + id + "/submit").with(csrf()).cookie(kovai)).andExpect(status().isOk());
		return id;
	}

	@Test
	void approvedProductIsVisibleToEveryone() throws Exception {
		long id = submitted("Kovai Yoga Mat");
		mvc.perform(get("/api/products/kovai-yoga-mat")).andExpect(status().isNotFound()); // not live yet

		mvc.perform(get("/api/admin/products").param("status", "PENDING").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + id + ")].name").value(org.hamcrest.Matchers.contains("Kovai Yoga Mat")));

		mvc.perform(post("/api/admin/products/" + id + "/approve").with(csrf()).cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		mvc.perform(get("/api/products/kovai-yoga-mat")).andExpect(status().isOk());

		assertThat(audit.latestFor("PRODUCT", id)).extracting(r -> r.getAction()).containsExactly("PRODUCT_APPROVED");
	}

	@Test
	void rejectedProductShowsTheReasonAndCanBeFixed() throws Exception {
		long id = submitted("Blurry Ball");
		mvc.perform(post("/api/admin/products/" + id + "/reject").with(csrf()).cookie(admin)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Please describe the ball size\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"));

		mvc.perform(get("/api/seller/products/" + id).cookie(kovai))
			.andExpect(jsonPath("$.rejectionReason").value("Please describe the ball size"));

		mvc.perform(put("/api/seller/products/" + id).with(csrf()).cookie(kovai).contentType(MediaType.APPLICATION_JSON)
				.content(SellerProducts.json(subCategory, "Size 5 Football", "999.00", "1299.00", 7)))
			.andExpect(status().isOk());
		mvc.perform(post("/api/seller/products/" + id + "/submit").with(csrf()).cookie(kovai))
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.rejectionReason").doesNotExist());

		assertThat(audit.latestFor("PRODUCT", id)).extracting(r -> r.getAction()).containsExactly("PRODUCT_REJECTED");
	}

	@Test
	void onlyPendingProductsCanBeDecided() throws Exception {
		long draft = SellerProducts.create(mvc, kovai, SellerProducts.json(subCategory, "Still a draft", "10.00", "10.00", 1));
		mvc.perform(post("/api/admin/products/" + draft + "/approve").with(csrf()).cookie(admin))
			.andExpect(status().isConflict());
	}

	/** A blocked shop's waiting products cannot go live. */
	@Test
	void productOfABlockedShopCannotBeApproved() throws Exception {
		long id = submitted("Blocked shop bat");
		jdbc.update("UPDATE sellers SET status = 'BLOCKED' WHERE slug = 'kovai-sports'");
		em.clear();
		mvc.perform(post("/api/admin/products/" + id + "/approve").with(csrf()).cookie(admin))
			.andExpect(status().isConflict());
	}

	@Test
	void sellersCannotApproveTheirOwnProducts() throws Exception {
		long id = submitted("Self approved");
		mvc.perform(post("/api/admin/products/" + id + "/approve").with(csrf()).cookie(kovai))
			.andExpect(status().isForbidden());
	}

}

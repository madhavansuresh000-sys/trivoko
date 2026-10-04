package com.trivoko.seller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** Phase 2 "done when": a shop that is not APPROVED cannot create products. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UnapprovedSellerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	private void tryToCreate(Cookie who) throws Exception {
		mvc.perform(post("/api/seller/products").with(csrf()).cookie(who).contentType(MediaType.APPLICATION_JSON)
				.content(SellerProducts.json(SellerProducts.subCategoryId(jdbc), "Not allowed", "100.00", "120.00", 5)))
			.andExpect(status().isForbidden());
	}

	/** Erode Organics is still PENDING: its owner is only a customer. */
	@Test
	void pendingShopCannotCreateProducts() throws Exception {
		tryToCreate(Logins.as(mvc, "erode.organics"));
	}

	/** Has the SELLER role, but the shop is REJECTED -> SellerAccess says no. */
	@Test
	void rejectedShopCannotCreateProducts() throws Exception {
		Cookie owner = Logins.as(mvc, "bengaluru.gadgets");
		jdbc.update("UPDATE sellers SET status = 'REJECTED' WHERE slug = 'bengaluru-gadget-hub'");
		em.clear(); // a real request starts with an empty Hibernate memory
		tryToCreate(owner);
	}

	@Test
	void blockedShopCannotCreateProducts() throws Exception {
		Cookie owner = Logins.as(mvc, "madurai.handlooms");
		jdbc.update("UPDATE sellers SET status = 'BLOCKED' WHERE slug = 'madurai-handlooms'");
		em.clear();
		tryToCreate(owner);
	}

	@Test
	void plainCustomerCannotCreateProducts() throws Exception {
		tryToCreate(Logins.as(mvc, "ravi"));
	}

}

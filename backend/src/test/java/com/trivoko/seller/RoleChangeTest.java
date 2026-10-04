package com.trivoko.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.admin.AuditService;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Spec decision P2-8: the roles come from the database on every request, so the SAME login cookie
 * becomes a seller's cookie the moment the admin approves, and stops working for selling the moment
 * the admin blocks the shop - no logout / login needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleChangeTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private AuditService audit;

	@Test
	void approvalAndBlockWorkAtOnceWithTheSameCookie() throws Exception {
		Cookie ravi = Logins.as(mvc, "ravi");
		Cookie admin = Logins.as(mvc, "admin");

		String json = mvc.perform(post("/api/seller/apply").with(csrf()).cookie(ravi).contentType(MediaType.APPLICATION_JSON)
				.content("{\"shopName\":\"Ravi Electronics\",\"city\":\"Chennai\"}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long shopId = ((Number) JsonPath.read(json, "$.id")).longValue();

		// still only a customer
		mvc.perform(get("/api/seller/products").cookie(ravi)).andExpect(status().isForbidden());

		mvc.perform(post("/api/admin/sellers/" + shopId + "/approve").with(csrf()).cookie(admin)).andExpect(status().isOk());
		mvc.perform(get("/api/seller/products").cookie(ravi)).andExpect(status().isOk());

		mvc.perform(post("/api/admin/sellers/" + shopId + "/block").with(csrf()).cookie(admin)).andExpect(status().isOk());
		mvc.perform(get("/api/seller/products").cookie(ravi)).andExpect(status().isForbidden());

		assertThat(audit.latestFor("SELLER", shopId))
			.extracting(row -> row.getAction())
			.containsExactly("SELLER_BLOCKED", "SELLER_APPROVED");
		assertThat(audit.latestFor("SELLER", shopId).get(0).getActorUserId()).isEqualTo(1L);
	}

}

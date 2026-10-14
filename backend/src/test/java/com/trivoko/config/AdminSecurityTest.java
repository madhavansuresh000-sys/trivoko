package com.trivoko.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 6: EVERY admin URL refuses a customer and a seller (403) and a visitor (401). One list, so a new admin
 * URL only needs one more line here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSecurityTest {

	private static final String REASON = "{\"reason\":\"Testing the guard\"}";

	@Autowired
	private MockMvc mvc;

	private static List<MockHttpServletRequestBuilder> adminCalls() {
		return List.of(
				get("/api/admin/overview"),
				get("/api/admin/sellers"),
				post("/api/admin/sellers/9/approve"),
				post("/api/admin/sellers/9/reject").contentType(MediaType.APPLICATION_JSON).content(REASON),
				post("/api/admin/sellers/1/block").contentType(MediaType.APPLICATION_JSON).content(REASON),
				post("/api/admin/sellers/1/unblock"),
				get("/api/admin/products"),
				get("/api/admin/products/1"),
				post("/api/admin/products/1/approve"),
				post("/api/admin/products/1/reject").contentType(MediaType.APPLICATION_JSON).content(REASON),
				get("/api/admin/users"),
				post("/api/admin/users/3/block"),
				post("/api/admin/users/3/unblock"),
				get("/api/admin/orders"),
				get("/api/admin/orders/TV-100000"),
				get("/api/admin/coupons"),
				post("/api/admin/coupons").contentType(MediaType.APPLICATION_JSON).content("{}"),
				put("/api/admin/coupons/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
				post("/api/admin/coupons/1/end"),
				get("/api/admin/reports/sales"),
				get("/api/admin/reports/top-products"),
				get("/api/admin/reports/top-sellers"),
				get("/api/admin/activity"));
	}

	@Test
	void customersGet403Everywhere() throws Exception {
		expectForAll(Logins.as(mvc, "ravi"), 403);
	}

	@Test
	void sellersGet403Everywhere() throws Exception {
		expectForAll(Logins.as(mvc, "chennai.mobiles"), 403);
	}

	@Test
	void visitorsGet401Everywhere() throws Exception {
		expectForAll(null, 401);
	}

	private void expectForAll(Cookie who, int expected) throws Exception {
		for (MockHttpServletRequestBuilder call : adminCalls()) {
			call.with(csrf());
			if (who != null) {
				call.cookie(who);
			}
			mvc.perform(call).andExpect(status().is(expected));
		}
	}

}

package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 8 step 6 - Kovai Sports says no; Ravi asks the admin once (within 3 days); the admin's word is final
 * (approve -> the return goes ahead, close -> closed for good) and lands in the activity log.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReturnEscalationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private Cookie kovai;

	private Cookie admin;

	private long returnId;

	@BeforeEach
	void kovaiRejectsRavisReturn() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		kovai = Logins.as(mvc, "kovai.sports");
		admin = Logins.as(mvc, "admin");
		long address = t.addressOf(2);
		t.addToCart(ravi, t.variantOf(2), 1);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse()
			.getContentAsString();
		String number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi)
			.with(csrf()));
		long pkg = jdbc.queryForObject("""
				SELECT p.id FROM packages p JOIN orders o ON o.id = p.order_id WHERE o.number = ? AND p.seller_id = 2""",
				Long.class, number);
		mvc.perform(post("/api/seller/packages/" + pkg + "/pack").cookie(kovai).with(csrf()));
		mvc.perform(post("/api/seller/packages/" + pkg + "/ship").cookie(kovai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNumber\":\"AWB112233\"}"));
		mvc.perform(post("/api/seller/packages/" + pkg + "/deliver").cookie(kovai).with(csrf()));
		long line = jdbc.queryForObject("SELECT MIN(id) FROM order_items WHERE package_id = ?", Long.class, pkg);
		String asked = mvc.perform(post("/api/me/returns").cookie(ravi).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"orderItemId\":" + line + ",\"reason\":\"DAMAGED\",\"note\":\"The zip is broken\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		returnId = ((Number) JsonPath.read(asked, "$.id")).longValue();
		mvc.perform(post("/api/seller/returns/" + returnId + "/reject").cookie(kovai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"The zip was fine when packed\"}"))
			.andExpect(status().isOk());
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM audit_log WHERE entity_type = 'RETURN'");
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	private ResultActions escalate(Cookie who) throws Exception {
		return mvc.perform(post("/api/me/returns/" + returnId + "/escalate").cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Here is a video of the broken zip\"}"));
	}

	@Test
	void theCustomerMayEscalateOnlyOnce() throws Exception {
		escalate(ravi)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ESCALATED"));
		assertThat(jdbc.queryForObject("SELECT escalated_at IS NOT NULL FROM return_requests WHERE id = ?",
				Boolean.class, returnId)).isTrue();
		escalate(ravi).andExpect(status().isConflict());
	}

	@Test
	void afterThreeDaysItIsTooLateToEscalate() throws Exception {
		jdbc.update("UPDATE return_requests SET updated_at = NOW(6) - INTERVAL 4 DAY WHERE id = ?", returnId);
		escalate(ravi).andExpect(status().isConflict());
	}

	@Test
	void onlyTheOwnerCanEscalate() throws Exception {
		escalate(Logins.as(mvc, "kavya")).andExpect(status().isNotFound());
	}

	@Test
	void theAdminApprovesAndItGoesAhead() throws Exception {
		escalate(ravi);
		mvc.perform(get("/api/admin/returns").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(returnId));

		mvc.perform(post("/api/admin/returns/" + returnId + "/approve").cookie(admin).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.events[3].actor").value("ADMIN"));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'RETURN_APPROVED' AND entity_id = ?",
				Integer.class, returnId)).isEqualTo(1);
	}

	@Test
	void theAdminClosesItForGood() throws Exception {
		escalate(ravi);
		mvc.perform(post("/api/admin/returns/" + returnId + "/close").cookie(admin).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Photos show normal use\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CLOSED"));
		escalate(ravi).andExpect(status().isConflict());
		mvc.perform(post("/api/admin/returns/" + returnId + "/approve").cookie(admin).with(csrf()))
			.andExpect(status().isConflict()); // final
	}

	@Test
	void theAdminCannotDecideBeforeAnEscalation() throws Exception {
		mvc.perform(post("/api/admin/returns/" + returnId + "/approve").cookie(admin).with(csrf()))
			.andExpect(status().isConflict()); // REJECTED -> APPROVED only through ESCALATED
	}

	@Test
	void onlyTheAdminDecides() throws Exception {
		escalate(ravi);
		mvc.perform(post("/api/admin/returns/" + returnId + "/approve").cookie(kovai).with(csrf()))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/admin/returns").cookie(ravi)).andExpect(status().isForbidden());
	}

}

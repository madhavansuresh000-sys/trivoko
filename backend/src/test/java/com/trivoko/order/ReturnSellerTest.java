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
import com.trivoko.returns.ReturnService;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 8 step 5 - the seller answers. Ravi returns one delivered line of Kovai Sports; Kovai approves or rejects
 * (with a reason); another shop cannot touch it; after 2 days with no answer the job approves it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReturnSellerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ReturnService returnService;

	private CheckoutTestSupport t;

	private Cookie kovai;

	private long returnId;

	@BeforeEach
	void raviAsksToReturnAKovaiLine() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		Cookie ravi = Logins.as(mvc, "ravi");
		kovai = Logins.as(mvc, "kovai.sports");
		long address = t.addressOf(2);
		long kovaiVariant = t.variantOf(2);
		t.addToCart(ravi, kovaiVariant, 1);
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
			.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNumber\":\"AWB556677\"}"));
		mvc.perform(post("/api/seller/packages/" + pkg + "/deliver").cookie(kovai).with(csrf()));
		long line = jdbc.queryForObject("SELECT MIN(id) FROM order_items WHERE package_id = ?", Long.class, pkg);
		String asked = mvc.perform(post("/api/me/returns").cookie(ravi).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"orderItemId\":" + line + ",\"reason\":\"SIZE_FIT\",\"note\":\"Too small for me\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		returnId = ((Number) JsonPath.read(asked, "$.id")).longValue();
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	private ResultActions reject(Cookie who, String reason) throws Exception {
		return mvc.perform(post("/api/seller/returns/" + returnId + "/reject").cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"));
	}

	@Test
	void theSellerSeesTheReturnInTheQueue() throws Exception {
		mvc.perform(get("/api/seller/returns").param("status", "REQUESTED").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(returnId));
		mvc.perform(get("/api/seller/returns").cookie(Logins.as(mvc, "chennai.mobiles")))
			.andExpect(jsonPath("$.content.length()").value(0)); // another shop sees nothing
	}

	@Test
	void approveMovesItOnceAndWritesTheTimeline() throws Exception {
		mvc.perform(post("/api/seller/returns/" + returnId + "/approve").cookie(kovai).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.events.length()").value(2))
			.andExpect(jsonPath("$.events[1].actor").value("SELLER"));
		mvc.perform(post("/api/seller/returns/" + returnId + "/approve").cookie(kovai).with(csrf()))
			.andExpect(status().isConflict()); // APPROVED -> APPROVED is not a move
	}

	@Test
	void rejectNeedsAReasonTheCustomerCanRead() throws Exception {
		reject(kovai, "no").andExpect(status().isBadRequest());
		reject(kovai, "Worn and washed, tags removed")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"))
			.andExpect(jsonPath("$.rejectReason").value("Worn and washed, tags removed"));
		mvc.perform(get("/api/me/returns/" + returnId).cookie(Logins.as(mvc, "ravi")))
			.andExpect(jsonPath("$.rejectReason").value("Worn and washed, tags removed"));
	}

	@Test
	void anotherShopCannotAnswer() throws Exception {
		mvc.perform(post("/api/seller/returns/" + returnId + "/approve").cookie(Logins.as(mvc, "chennai.mobiles"))
			.with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(post("/api/seller/returns/" + returnId + "/approve").cookie(Logins.as(mvc, "ravi")).with(csrf()))
			.andExpect(status().isForbidden()); // a customer is not a seller
	}

	@Test
	void twoDaysWithoutAnAnswerMeansYes() {
		assertThat(returnService.autoApproveOld()).isZero(); // asked just now: still waiting

		jdbc.update("UPDATE return_requests SET created_at = NOW(6) - INTERVAL 3 DAY WHERE id = ?", returnId);
		assertThat(returnService.autoApproveOld()).isEqualTo(1);

		assertThat(jdbc.queryForObject("SELECT status FROM return_requests WHERE id = ?", String.class, returnId))
			.isEqualTo("APPROVED");
		assertThat(jdbc.queryForObject("SELECT actor FROM return_events WHERE return_id = ? ORDER BY id DESC LIMIT 1",
				String.class, returnId)).isEqualTo("SYSTEM");
	}

}

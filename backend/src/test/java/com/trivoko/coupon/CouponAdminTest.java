package com.trivoko.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

import com.trivoko.admin.AuditService;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** Phase 6: the admin edits and ends coupons. An ended coupon stops working at checkout at once. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponAdminTest {

	private static final long RAVI = 2;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CouponService coupons;

	@Autowired
	private AuditService audit;

	@Autowired
	private JdbcTemplate jdbc;

	private Cookie admin;

	private long flat100;

	@BeforeEach
	void login() throws Exception {
		admin = Logins.as(mvc, "admin");
		flat100 = jdbc.queryForObject("SELECT id FROM coupons WHERE code = 'FLAT100'", Long.class);
	}

	private ResultActions edit(long id, String value) throws Exception {
		String until = LocalDateTime.now().plusDays(30).withNano(0).toString();
		return mvc.perform(put("/api/admin/coupons/" + id).with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON)
			.content("{\"description\":\"₹150 off on orders from ₹999\",\"value\":" + value
					+ ",\"minOrder\":999,\"validUntil\":\"" + until + "\"}"));
	}

	@Test
	void editingChangesTheDiscount() throws Exception {
		edit(flat100, "150").andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("FLAT100"))
			.andExpect(jsonPath("$.value").value(150.00));
		assertThat(coupons.quote("FLAT100", RAVI, new BigDecimal("1200"), false).discount()).isEqualByComparingTo("150.00");
		assertThat(audit.latestFor("COUPON", flat100)).extracting(l -> l.getAction()).contains("COUPON_UPDATED");
	}

	@Test
	void endedCouponIsRefusedAtCheckout() throws Exception {
		mvc.perform(post("/api/admin/coupons/" + flat100 + "/end").with(csrf()).cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
		assertThat(coupons.quote("FLAT100", RAVI, new BigDecimal("1200"), false).error())
			.isEqualTo("This coupon is no longer available");
		assertThat(audit.latestFor("COUPON", flat100)).extracting(l -> l.getAction()).contains("COUPON_ENDED");
	}

	@Test
	void anEndedCouponCannotBeEndedOrEditedAgain() throws Exception {
		mvc.perform(post("/api/admin/coupons/" + flat100 + "/end").with(csrf()).cookie(admin)).andExpect(status().isOk());
		mvc.perform(post("/api/admin/coupons/" + flat100 + "/end").with(csrf()).cookie(admin)).andExpect(status().isConflict());
		edit(flat100, "150").andExpect(status().isConflict());
	}

	@Test
	void editNeedsAValue() throws Exception {
		edit(flat100, "0").andExpect(status().isBadRequest());
	}

	@Test
	void unknownCouponIsNotFound() throws Exception {
		mvc.perform(post("/api/admin/coupons/999999/end").with(csrf()).cookie(admin)).andExpect(status().isNotFound());
	}

}

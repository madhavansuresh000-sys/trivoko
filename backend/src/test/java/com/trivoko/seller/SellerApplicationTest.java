package com.trivoko.seller;

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

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** "Become a seller" (POST /api/seller/apply) and the admin's approve / reject / block. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerApplicationTest {

	@Autowired
	private MockMvc mvc;

	private Cookie ravi;

	private Cookie admin;

	@BeforeEach
	void login() throws Exception {
		ravi = Logins.as(mvc, "ravi");
		admin = Logins.as(mvc, "admin");
	}

	private ResultActions apply(Cookie who, String shopName) throws Exception {
		return mvc.perform(post("/api/seller/apply").with(csrf()).cookie(who).contentType(MediaType.APPLICATION_JSON)
			.content("{\"shopName\":\"" + shopName + "\",\"city\":\"Chennai\",\"description\":\"Gadgets\",\"gstin\":\"\"}"));
	}

	private long applyId(Cookie who, String shopName) throws Exception {
		String json = apply(who, shopName).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private ResultActions adminDoes(String action, long sellerId, String body) throws Exception {
		var request = post("/api/admin/sellers/" + sellerId + "/" + action).with(csrf()).cookie(admin);
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mvc.perform(request);
	}

	@Test
	void applyingCreatesAPendingShop() throws Exception {
		apply(ravi, "Ravi's Gadget Shop")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.slug").value("ravi-s-gadget-shop"));
		mvc.perform(get("/api/seller/application").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shopName").value("Ravi's Gadget Shop"));
	}

	@Test
	void noApplicationYetIsNotFound() throws Exception {
		mvc.perform(get("/api/seller/application").cookie(ravi)).andExpect(status().isNotFound());
	}

	@Test
	void applyingTwiceIsRefused() throws Exception {
		applyId(ravi, "Ravi Shop");
		apply(ravi, "Ravi Shop Again").andExpect(status().isConflict());
	}

	/** Review focus: a name whose slug is taken gets -2. */
	@Test
	void takenSlugGetsANumber() throws Exception {
		apply(ravi, "Chennai Mobiles!").andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("chennai-mobiles-2"));
	}

	@Test
	void badGstinIsRejected() throws Exception {
		mvc.perform(post("/api/seller/apply").with(csrf()).cookie(ravi).contentType(MediaType.APPLICATION_JSON)
				.content("{\"shopName\":\"Ravi Shop\",\"city\":\"Chennai\",\"gstin\":\"12AB\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.gstin").exists());
	}

	@Test
	void rejectedApplicantMayApplyAgain() throws Exception {
		long id = applyId(ravi, "Ravi Shop");
		adminDoes("reject", id, "{\"reason\":\"Please add your GST number\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"));
		mvc.perform(get("/api/seller/application").cookie(ravi))
			.andExpect(jsonPath("$.rejectReason").value("Please add your GST number"));

		apply(ravi, "Ravi Shop").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.rejectReason").doesNotExist());
	}

	@Test
	void blockedSellerMayNotApplyAgain() throws Exception {
		Cookie kovai = Logins.as(mvc, "kovai.sports");
		adminDoes("block", 2, null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
		apply(kovai, "Kovai Sports New").andExpect(status().isForbidden());
	}

	@Test
	void onlyPendingShopsCanBeApproved() throws Exception {
		adminDoes("approve", 1, null).andExpect(status().isConflict()); // Chennai Mobiles is already APPROVED
	}

	@Test
	void adminListsPendingShops() throws Exception {
		mvc.perform(get("/api/admin/sellers").param("status", "PENDING").cookie(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].slug").value(org.hamcrest.Matchers.contains("erode-organics")));
	}

	@Test
	void customersCannotUseAdminUrls() throws Exception {
		mvc.perform(get("/api/admin/sellers").cookie(ravi)).andExpect(status().isForbidden());
		mvc.perform(post("/api/admin/sellers/9/approve").with(csrf()).cookie(ravi)).andExpect(status().isForbidden());
	}

}

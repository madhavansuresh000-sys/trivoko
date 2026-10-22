package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
 * Phase 8 step 4 - asking for a return. Ravi buys from Kovai Sports (delivered) and Chennai Mobiles (still
 * placed). Rules: own line only, delivered, within 7 days, once per line; refund = line total - its coupon share.
 * (Lives in the order package to reuse CheckoutTestSupport.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReturnRequestTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private long kovaiLine;

	private long chennaiLine;

	private long kovaiPackage;

	@BeforeEach
	void raviHasOneDeliveredAndOnePlacedPackage() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		Cookie kovai = Logins.as(mvc, "kovai.sports");
		long address = t.addressOf(2);
		long chennaiVariant = t.variantOf(1);
		long kovaiVariant = t.variantOf(2);
		t.addToCart(ravi, chennaiVariant, 1);
		t.addToCart(ravi, kovaiVariant, 2);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse()
			.getContentAsString();
		String number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi)
			.with(csrf()));
		kovaiPackage = packageOf(number, 2);
		long chennaiPackage = packageOf(number, 1);
		kovaiLine = lineOf(kovaiPackage);
		chennaiLine = lineOf(chennaiPackage);
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/pack").cookie(kovai).with(csrf()))
			.andExpect(status().isOk());
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/ship").cookie(kovai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNumber\":\"AWB778899\"}"))
			.andExpect(status().isOk());
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/deliver").cookie(kovai).with(csrf()))
			.andExpect(status().isOk());
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	private long packageOf(String number, long sellerId) {
		return jdbc.queryForObject("""
				SELECT p.id FROM packages p JOIN orders o ON o.id = p.order_id WHERE o.number = ? AND p.seller_id = ?""",
				Long.class, number, sellerId);
	}

	private long lineOf(long packageId) {
		return jdbc.queryForObject("SELECT MIN(id) FROM order_items WHERE package_id = ?", Long.class, packageId);
	}

	private ResultActions ask(Cookie who, long line, String note, String photo) throws Exception {
		String body = "{\"orderItemId\":" + line + ",\"reason\":\"DAMAGED\",\"note\":\"" + note + "\""
				+ (photo == null ? "" : ",\"photoUrl\":\"" + photo + "\"") + "}";
		return mvc.perform(post("/api/me/returns").cookie(who).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	@Test
	void aDeliveredLineCanBeReturnedForWhatWasPaidForIt() throws Exception {
		BigDecimal paid = jdbc.queryForObject("SELECT line_total - discount_share FROM order_items WHERE id = ?",
				BigDecimal.class, kovaiLine);

		String json = ask(ravi, kovaiLine, "The strap arrived torn", null)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("REQUESTED"))
			.andExpect(jsonPath("$.events.length()").value(1))
			.andExpect(jsonPath("$.events[0].actor").value("CUSTOMER"))
			.andReturn().getResponse().getContentAsString();

		assertThat(new BigDecimal(JsonPath.read(json, "$.refundAmount").toString())).isEqualByComparingTo(paid);
		assertThat(jdbc.queryForObject("SELECT seller_id FROM return_requests", Long.class)).isEqualTo(2L);
		mvc.perform(get("/api/me/returns").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void aLineNotYetDeliveredCannotBeReturned() throws Exception {
		ask(ravi, chennaiLine, "Changed my mind about it", null).andExpect(status().isConflict());
	}

	@Test
	void afterSevenDaysTheWindowIsClosed() throws Exception {
		jdbc.update("UPDATE packages SET delivered_at = NOW(6) - INTERVAL 8 DAY WHERE id = ?", kovaiPackage);
		ask(ravi, kovaiLine, "Too late but trying", null).andExpect(status().isConflict());
	}

	@Test
	void oneReturnPerLine() throws Exception {
		ask(ravi, kovaiLine, "The strap arrived torn", null).andExpect(status().isCreated());
		ask(ravi, kovaiLine, "Asking a second time", null).andExpect(status().isConflict());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM return_requests", Integer.class)).isEqualTo(1);
	}

	@Test
	void someoneElsesLineIsNotFound() throws Exception {
		ask(Logins.as(mvc, "kavya"), kovaiLine, "Not my order at all", null).andExpect(status().isNotFound());
	}

	@Test
	void aShortNoteOrAForeignPhotoIsRefused() throws Exception {
		ask(ravi, kovaiLine, "bad", null).andExpect(status().isBadRequest());
		ask(ravi, kovaiLine, "The strap arrived torn", "https://example.com/photo.jpg").andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM return_requests", Integer.class)).isZero();
	}

	@Test
	void theCustomerCanCancelBeforeTheSellerAnswersButOnlyOnce() throws Exception {
		String json = ask(ravi, kovaiLine, "The strap arrived torn", null).andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(json, "$.id");

		mvc.perform(post("/api/me/returns/" + id + "/cancel").cookie(ravi).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.events.length()").value(2));
		mvc.perform(post("/api/me/returns/" + id + "/cancel").cookie(ravi).with(csrf()))
			.andExpect(status().isConflict()); // CANCELLED is an end state
		mvc.perform(get("/api/me/returns/" + id).cookie(Logins.as(mvc, "kavya")))
			.andExpect(status().isNotFound());
	}

}

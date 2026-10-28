package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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
 * Phase 9 step 2 - verified-buyer reviews. Ravi buys from Kovai Sports (delivered) and Chennai Mobiles (still
 * placed). Only a DELIVERED product may be reviewed, once per customer; the product keeps its average and count.
 * (Lives in the order package to reuse CheckoutTestSupport.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReviewTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private String delivered;

	private String notYetDelivered;

	@BeforeEach
	void raviHasOneDeliveredAndOnePlacedPackage() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		Cookie kovai = Logins.as(mvc, "kovai.sports");
		long address = t.addressOf(2);
		long chennaiVariant = t.variantOf(1);
		long kovaiVariant = t.variantOf(2);
		t.addToCart(ravi, chennaiVariant, 1);
		t.addToCart(ravi, kovaiVariant, 1);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse()
			.getContentAsString();
		String number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi)
			.with(csrf()));
		long kovaiPackage = jdbc.queryForObject("""
				SELECT p.id FROM packages p JOIN orders o ON o.id = p.order_id WHERE o.number = ? AND p.seller_id = 2""",
				Long.class, number);
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/pack").cookie(kovai).with(csrf()))
			.andExpect(status().isOk());
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/ship").cookie(kovai).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNumber\":\"AWB445566\"}"))
			.andExpect(status().isOk());
		mvc.perform(post("/api/seller/packages/" + kovaiPackage + "/deliver").cookie(kovai).with(csrf()))
			.andExpect(status().isOk());
		delivered = slugOf(kovaiVariant);
		notYetDelivered = slugOf(chennaiVariant);
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM reviews");
		jdbc.update("UPDATE products SET rating_avg = NULL, rating_count = 0");
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	private String slugOf(long variantId) {
		return jdbc.queryForObject(
				"SELECT p.slug FROM products p JOIN product_variants v ON v.product_id = p.id WHERE v.id = ?",
				String.class, variantId);
	}

	private Map<String, Object> ratingOf(String slug) {
		return jdbc.queryForMap("SELECT rating_avg, rating_count FROM products WHERE slug = ?", slug);
	}

	private ResultActions write(Cookie who, String slug, int rating, String body) throws Exception {
		return mvc.perform(put("/api/products/" + slug + "/reviews/mine").cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"rating\":" + rating + ",\"title\":\"My view\",\"body\":\"" + body + "\"}"));
	}

	@Test
	void aDeliveredProductCanBeReviewedAndThePageShowsIt() throws Exception {
		mvc.perform(get("/api/products/" + delivered + "/reviews/mine").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canReview").value(true))
			.andExpect(jsonPath("$.review").doesNotExist());

		write(ravi, delivered, 4, "Good grip, runs a little small").andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(4))
			.andExpect(jsonPath("$.author").value("Ravi K."));

		assertThat(ratingOf(delivered).get("rating_avg").toString()).isEqualTo("4.0");
		assertThat(ratingOf(delivered).get("rating_count")).isEqualTo(1);

		// the product page is public: no cookie
		mvc.perform(get("/api/products/" + delivered + "/reviews"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary.average").value(4.0))
			.andExpect(jsonPath("$.summary.count").value(1))
			.andExpect(jsonPath("$.summary.stars['4']").value(1))
			.andExpect(jsonPath("$.summary.stars['5']").value(0))
			.andExpect(jsonPath("$.reviews.content[0].author").value("Ravi K."))
			.andExpect(jsonPath("$.reviews.content[0].verifiedBuyer").value(true))
			.andExpect(jsonPath("$.reviews.content[0].body").value("Good grip, runs a little small"));
	}

	@Test
	void notDeliveredOrNeverBoughtMayNotReview() throws Exception {
		write(ravi, notYetDelivered, 5, "Not here yet but excited").andExpect(status().isForbidden());
		mvc.perform(get("/api/products/" + notYetDelivered + "/reviews/mine").cookie(ravi))
			.andExpect(jsonPath("$.canReview").value(false));

		Cookie kavya = Logins.as(mvc, "kavya");
		write(kavya, delivered, 1, "Never bought it, still angry").andExpect(status().isForbidden());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isZero();
	}

	@Test
	void writingAgainEditsTheSameReview() throws Exception {
		write(ravi, delivered, 5, "Love it on the first day").andExpect(status().isOk());
		write(ravi, delivered, 3, "After a month the sole wore out").andExpect(status().isOk());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT rating FROM reviews", Integer.class)).isEqualTo(3);
		assertThat(ratingOf(delivered).get("rating_avg").toString()).isEqualTo("3.0");
		assertThat(ratingOf(delivered).get("rating_count")).isEqualTo(1);
	}

	@Test
	void averageIsRoundedToOneDecimalAndTheBarsCountEachStar() throws Exception {
		long productId = jdbc.queryForObject("SELECT id FROM products WHERE slug = ?", Long.class, delivered);
		// two earlier reviews (written straight into the table: kavya and the admin "bought" it before)
		jdbc.update("INSERT INTO reviews (product_id, user_id, rating, body) VALUES (?, 3, 4, 'Nice and light')",
				productId);
		jdbc.update("INSERT INTO reviews (product_id, user_id, rating, body) VALUES (?, 1, 4, 'Good for running')",
				productId);

		write(ravi, delivered, 5, "Best shoes I have bought").andExpect(status().isOk());

		// (4 + 4 + 5) / 3 = 4.333 -> 4.3
		assertThat(ratingOf(delivered).get("rating_avg").toString()).isEqualTo("4.3");
		mvc.perform(get("/api/products/" + delivered + "/reviews"))
			.andExpect(jsonPath("$.summary.average").value(4.3))
			.andExpect(jsonPath("$.summary.count").value(3))
			.andExpect(jsonPath("$.summary.stars['5']").value(1))
			.andExpect(jsonPath("$.summary.stars['4']").value(2))
			.andExpect(jsonPath("$.summary.stars['1']").value(0))
			.andExpect(jsonPath("$.reviews.content.length()").value(3));
	}

	@Test
	void theAuthorCanDeleteTheirReview() throws Exception {
		write(ravi, delivered, 2, "Too tight for my feet").andExpect(status().isOk());

		mvc.perform(delete("/api/products/" + delivered + "/reviews/mine").cookie(ravi).with(csrf()))
			.andExpect(status().isNoContent());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isZero();
		assertThat(ratingOf(delivered).get("rating_avg")).isNull();
		assertThat(ratingOf(delivered).get("rating_count")).isEqualTo(0);
		mvc.perform(delete("/api/products/" + delivered + "/reviews/mine").cookie(ravi).with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void ratingAndTextAreChecked() throws Exception {
		write(ravi, delivered, 0, "Zero stars is not a thing").andExpect(status().isBadRequest());
		write(ravi, delivered, 6, "Six stars is not a thing").andExpect(status().isBadRequest());
		write(ravi, delivered, 4, "Too short").andExpect(status().isBadRequest());
	}

	@Test
	void aGuestMayReadReviewsButNotAskAboutTheirOwn() throws Exception {
		mvc.perform(get("/api/products/" + delivered + "/reviews")).andExpect(status().isOk());
		mvc.perform(get("/api/products/" + delivered + "/reviews/mine")).andExpect(status().isUnauthorized());
	}

}

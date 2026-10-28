package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 10 step 6 - "Customers also bought": products that were in the same PAID orders as this one, most often
 * first, cached for 1 hour. (Lives in the order package to reuse CheckoutTestSupport.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlsoBoughtTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private CacheManager caches;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private long address;

	@BeforeEach
	void setUp() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		address = t.addressOf(2);
		caches.getCache("alsoBought").clear();
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications");
		t.clean();
		caches.getCache("alsoBought").clear();
	}

	/** Ravi orders these variants together; paid = true goes through the fake payment page. */
	private void ravisOrder(boolean paid, long... variantIds) throws Exception {
		for (long v : variantIds) {
			t.addToCart(ravi, v, 1);
		}
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse()
			.getContentAsString();
		if (paid) {
			String url = JsonPath.read(json, "$.redirectUrl");
			mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi)
				.with(csrf()));
		}
	}

	private String slugOf(long variantId) {
		return jdbc.queryForObject(
				"SELECT p.slug FROM products p JOIN product_variants v ON v.product_id = p.id WHERE v.id = ?",
				String.class, variantId);
	}

	private List<String> alsoBought(String slug) throws Exception {
		String json = mvc.perform(get("/api/products/" + slug + "/also-bought")).andExpect(status().isOk()).andReturn()
			.getResponse().getContentAsString();
		return JsonPath.read(json, "$[*].slug");
	}

	@Test
	void productsFromTheSamePaidOrderShowUp() throws Exception {
		long phone = t.variantOf(1);
		long shoes = t.variantOf(2);
		ravisOrder(true, phone, shoes);

		assertThat(alsoBought(slugOf(phone))).containsExactly(slugOf(shoes));
		assertThat(alsoBought(slugOf(shoes))).containsExactly(slugOf(phone));
	}

	@Test
	void anUnpaidOrderDoesNotCount() throws Exception {
		long phone = t.variantOf(1);
		long shoes = t.variantOf(2);
		ravisOrder(false, phone, shoes);

		assertThat(alsoBought(slugOf(phone))).isEmpty();
	}

	@Test
	void theAnswerIsKeptForAnHour() throws Exception {
		long phone = t.variantOf(1);
		long shoes = t.variantOf(2);
		assertThat(alsoBought(slugOf(phone))).isEmpty(); // asked once: "nothing" is now remembered

		ravisOrder(true, phone, shoes);
		assertThat(alsoBought(slugOf(phone))).isEmpty(); // still the remembered answer

		CaffeineCache cache = (CaffeineCache) caches.getCache("alsoBought");
		assertThat(cache.getNativeCache().policy().expireAfterWrite().orElseThrow().getExpiresAfter())
			.isEqualTo(Duration.ofHours(1));
		cache.clear();
		assertThat(alsoBought(slugOf(phone))).containsExactly(slugOf(shoes));
	}

	@Test
	void anUnknownProductIsNotFound() throws Exception {
		mvc.perform(get("/api/products/no-such-thing/also-bought")).andExpect(status().isNotFound());
	}

}

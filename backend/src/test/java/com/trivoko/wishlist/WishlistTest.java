package com.trivoko.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * Phase 9 step 3 - the wishlist. Saved per product at its "from" price; adding twice keeps one row; a product
 * that stops being live stays in the list as "not available". Not one rolled-back transaction: each request must
 * see what really reached the database, so clean() puts things back.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WishlistTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private Cookie ravi;

	private long volta;

	private BigDecimal voltaPrice;

	@BeforeEach
	void setUp() throws Exception {
		ravi = Logins.as(mvc, "ravi");
		volta = jdbc.queryForObject("SELECT id FROM products WHERE slug = 'volta-v12-5g'", Long.class);
		voltaPrice = jdbc.queryForObject("SELECT price_from FROM products WHERE id = ?", BigDecimal.class, volta);
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM wishlist_items");
		jdbc.update("UPDATE products SET status = 'ACTIVE', price_from = ? WHERE id = ?", voltaPrice, volta);
	}

	private ResultActions add(Cookie who, long productId) throws Exception {
		return mvc.perform(put("/api/me/wishlist/" + productId).cookie(who).with(csrf()));
	}

	@Test
	void addingTwiceKeepsOneRowAtTheFromPrice() throws Exception {
		add(ravi, volta).andExpect(status().isNoContent());
		add(ravi, volta).andExpect(status().isNoContent());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM wishlist_items", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT price_when_added FROM wishlist_items", BigDecimal.class))
			.isEqualByComparingTo(voltaPrice);
		mvc.perform(get("/api/me/wishlist/ids").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]").value(volta));
	}

	@Test
	void theListShowsHowMuchCheaperItIsNow() throws Exception {
		add(ravi, volta);
		jdbc.update("UPDATE products SET price_from = price_from - 1000 WHERE id = ?", volta);

		mvc.perform(get("/api/me/wishlist").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].product.slug").value("volta-v12-5g"))
			.andExpect(jsonPath("$[0].available").value(true))
			.andExpect(jsonPath("$[0].priceWhenAdded").value(voltaPrice.doubleValue()))
			.andExpect(jsonPath("$[0].priceDrop").value(1000.0));
	}

	@Test
	void aProductThatIsNoLongerLiveStaysButIsMarkedNotAvailable() throws Exception {
		add(ravi, volta);
		jdbc.update("UPDATE products SET status = 'BLOCKED' WHERE id = ?", volta);

		mvc.perform(get("/api/me/wishlist").cookie(ravi))
			.andExpect(jsonPath("$[0].product.id").value(volta))
			.andExpect(jsonPath("$[0].available").value(false));
		// ... but a product that is not live cannot be ADDED
		jdbc.update("DELETE FROM wishlist_items");
		add(ravi, volta).andExpect(status().isNotFound());
	}

	@Test
	void removeTakesItOutAndIsSafeToRepeat() throws Exception {
		add(ravi, volta);
		mvc.perform(delete("/api/me/wishlist/" + volta).cookie(ravi).with(csrf())).andExpect(status().isNoContent());
		mvc.perform(delete("/api/me/wishlist/" + volta).cookie(ravi).with(csrf())).andExpect(status().isNoContent());
		mvc.perform(get("/api/me/wishlist/ids").cookie(ravi)).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void everyCustomerHasTheirOwnList() throws Exception {
		add(ravi, volta);
		Cookie kavya = Logins.as(mvc, "kavya");
		mvc.perform(get("/api/me/wishlist").cookie(kavya)).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void atMostOneHundredItems() throws Exception {
		jdbc.update("""
				INSERT INTO wishlist_items (user_id, product_id, price_when_added)
				SELECT 2, id, price_from FROM products WHERE id <> ? AND status = 'ACTIVE' ORDER BY id LIMIT 100""", volta);

		add(ravi, volta).andExpect(status().isConflict());
	}

	@Test
	void aGuestIsAskedToLogIn() throws Exception {
		mvc.perform(get("/api/me/wishlist")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/me/wishlist/" + volta).with(csrf())).andExpect(status().isUnauthorized());
	}

}

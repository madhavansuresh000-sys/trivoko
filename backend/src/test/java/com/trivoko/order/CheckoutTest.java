package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
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

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** Checkout from cart to "please pay" (fake payment page), against the seed data. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private long address;

	private long chennai;

	private long kovai;

	@BeforeEach
	void ravisCartWithTwoSellers() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		address = t.addressOf(2);
		chennai = t.variantOf(1);
		kovai = t.variantOf(2);
		t.addToCart(ravi, chennai, 2);
		t.addToCart(ravi, kovai, 1);
	}

	@AfterEach
	void clean() {
		t.clean();
	}

	@Test
	void previewShowsOnePackagePerSellerAndTheCoupon() throws Exception {
		BigDecimal items = t.price(chennai).multiply(BigDecimal.TWO).add(t.price(kovai));
		t.preview(ravi, null, "welcome10")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.address.id").value(address)) // no address chosen -> my default
			.andExpect(jsonPath("$.packages.length()").value(2))
			.andExpect(jsonPath("$.packages[0].sellerSlug").value("chennai-mobiles"))
			.andExpect(jsonPath("$.itemsTotal").value(items.doubleValue()))
			.andExpect(jsonPath("$.coupon.code").value("WELCOME10"))
			.andExpect(jsonPath("$.coupon.error").doesNotExist());
		assertThat(t.orderCount()).isZero(); // a preview saves nothing
	}

	@Test
	void badCouponIsShownNotThrown() throws Exception {
		t.preview(ravi, address, "NOPE")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.coupon.error").value("This coupon does not exist"))
			.andExpect(jsonPath("$.discountTotal").value(0));
	}

	/** Phase 4 "done when" (first half): one order, 2 packages, stock held, waiting for payment. */
	@Test
	void placeOrderHoldsStockAndOpensThePaymentPage() throws Exception {
		int chennaiStock = t.stock(chennai);
		String total = t.previewTotal(ravi, address, "WELCOME10");

		String json = t.place(ravi, address, "WELCOME10", total)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.number", matchesPattern("TV-\\d{6}")))
			.andExpect(jsonPath("$.redirectUrl", containsString("/test-payment/fake_cs_")))
			.andReturn().getResponse().getContentAsString();

		String number = JsonPath.read(json, "$.number");
		assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, number))
			.isEqualTo("PENDING_PAYMENT");
		assertThat(jdbc.queryForObject("""
				SELECT COUNT(*) FROM packages p JOIN orders o ON o.id = p.order_id
				WHERE o.number = ? AND p.status = 'PENDING_PAYMENT'""", Integer.class, number)).isEqualTo(2);
		assertThat(jdbc.queryForObject("SELECT grand_total FROM orders WHERE number = ?", BigDecimal.class, number))
			.isEqualByComparingTo(total);
		assertThat(t.stock(chennai)).isEqualTo(chennaiStock - 2);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE status = 'CREATED'", Integer.class)).isEqualTo(1);
	}

	/** Review focus: the price changed between "look" and "pay" -> nothing is held, the customer re-checks. */
	@Test
	void priceChangedIsRefused() throws Exception {
		String total = t.previewTotal(ravi, address, null);
		int stock = t.stock(chennai);
		jdbc.update("UPDATE product_variants SET price = price + 1 WHERE id = ?", chennai);
		try {
			t.place(ravi, address, null, total)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("Prices changed")));
			assertThat(t.orderCount()).isZero();
			assertThat(t.stock(chennai)).isEqualTo(stock);
		}
		finally {
			jdbc.update("UPDATE product_variants SET price = price - 1 WHERE id = ?", chennai);
		}
	}

	/** Review focus: one line runs out during the hold -> EVERY hold is undone. */
	@Test
	void ranOutRollsBackEveryHold() throws Exception {
		String total = t.previewTotal(ravi, address, null);
		int chennaiStock = t.stock(chennai);
		// Kovai's shoes sell out after Ravi looked (his preview still showed them)
		jdbc.update("UPDATE product_variants SET stock = 0 WHERE id = ?", kovai);
		t.place(ravi, address, null, total)
			.andExpect(status().isConflict());
		assertThat(t.orderCount()).isZero();
		assertThat(t.stock(chennai)).isEqualTo(chennaiStock);
	}

	/** Review focus: Pay pressed twice (two tabs) -> one order, the same payment page. */
	@Test
	void secondCheckoutReusesPendingOrder() throws Exception {
		String total = t.previewTotal(ravi, address, null);
		String first = t.place(ravi, address, null, total).andReturn().getResponse().getContentAsString();
		int stockAfterFirst = t.stock(chennai);

		String second = t.place(ravi, address, null, total)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();

		assertThat((String) JsonPath.read(second, "$.number")).isEqualTo(JsonPath.read(first, "$.number"));
		assertThat((String) JsonPath.read(second, "$.redirectUrl")).isEqualTo(JsonPath.read(first, "$.redirectUrl"));
		assertThat(t.orderCount()).isEqualTo(1);
		assertThat(t.stock(chennai)).isEqualTo(stockAfterFirst); // not held twice
	}

	/** Final review: the cart CHANGED after an unpaid order -> the old order is replaced, never paid by mistake. */
	@Test
	void changedCartReplacesThePendingOrder() throws Exception {
		String firstTotal = t.previewTotal(ravi, address, null);
		String first = t.place(ravi, address, null, firstTotal).andReturn().getResponse().getContentAsString();
		int chennaiAfterFirst = t.stock(chennai);

		t.addToCart(ravi, chennai, 3); // he changes his mind: 3 cases instead of 2
		String secondTotal = t.previewTotal(ravi, address, null);
		String second = t.place(ravi, address, null, secondTotal)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();

		String firstNumber = JsonPath.read(first, "$.number");
		assertThat((String) JsonPath.read(second, "$.number")).isNotEqualTo(firstNumber);
		assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, firstNumber))
			.isEqualTo("EXPIRED");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE status = 'PENDING_PAYMENT'", Integer.class))
			.isEqualTo(1);
		assertThat(t.stock(chennai)).isEqualTo(chennaiAfterFirst + 2 - 3); // old hold released, new one taken
	}

	@Test
	void invalidCouponCannotBePlaced() throws Exception {
		String total = t.previewTotal(ravi, address, null);
		t.place(ravi, address, "NOPE", total)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("This coupon does not exist"));
	}

	@Test
	void someoneElsesAddressIsNotFound() throws Exception {
		long kavyasAddress = t.addressOf(3);
		t.place(ravi, kavyasAddress, null, "1").andExpect(status().isNotFound());
	}

	@Test
	void emptyCartCannotBePlaced() throws Exception {
		Cookie kavya = Logins.as(mvc, "kavya");
		long kavyasAddress = t.addressOf(3);
		t.place(kavya, kavyasAddress, null, "0")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Your cart is empty."));
	}

	@Test
	void checkoutNeedsLogin() throws Exception {
		t.place(null, address, null, "1").andExpect(status().isUnauthorized());
	}

}

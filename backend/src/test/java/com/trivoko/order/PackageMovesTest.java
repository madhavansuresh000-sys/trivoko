package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
 * The seller's packing table: PLACED -> PACKED -> SHIPPED -> DELIVERED, and cancelling before shipping
 * (stock back + a partial refund of just that package). Ravi buys from Chennai Mobiles AND Kovai Sports.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PackageMovesTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private Cookie kovai;

	private String number;

	private long kovaiPackage;

	private long chennaiPackage;

	private long kovaiVariant;

	@BeforeEach
	void ravisPaidOrderFromTwoSellers() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		kovai = Logins.as(mvc, "kovai.sports");
		long address = t.addressOf(2);
		long chennaiVariant = t.variantOf(1);
		kovaiVariant = t.variantOf(2);
		t.addToCart(ravi, chennaiVariant, 1);
		t.addToCart(ravi, kovaiVariant, 2);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null)).andReturn().getResponse().getContentAsString();
		number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		mvc.perform(post("/api/payments/fake/" + url.substring(url.lastIndexOf('/') + 1) + "/complete").cookie(ravi).with(csrf()));
		kovaiPackage = packageOf(2);
		chennaiPackage = packageOf(1);
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	private long packageOf(long sellerId) {
		return jdbc.queryForObject("""
				SELECT p.id FROM packages p JOIN orders o ON o.id = p.order_id WHERE o.number = ? AND p.seller_id = ?""",
				Long.class, number, sellerId);
	}

	private String packageStatus(long id) {
		return jdbc.queryForObject("SELECT status FROM packages WHERE id = ?", String.class, id);
	}

	private ResultActions move(Cookie who, long id, String action, String json) throws Exception {
		var request = post("/api/seller/packages/" + id + "/" + action).cookie(who).with(csrf());
		return mvc.perform(json == null ? request : request.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void sellerSeesOnlyTheirOwnPackages() throws Exception {
		mvc.perform(get("/api/seller/packages").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(kovaiPackage))
			.andExpect(jsonPath("$.content[0].orderNumber").value(number))
			.andExpect(jsonPath("$.content[0].status").value("PLACED"))
			.andExpect(jsonPath("$.content[0].items[0].quantity").value(2));
	}

	@Test
	void packShipDeliver() throws Exception {
		move(kovai, kovaiPackage, "pack", null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PACKED"));
		move(kovai, kovaiPackage, "ship", "{\"trackingNumber\":\"AWB12345\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SHIPPED"))
			.andExpect(jsonPath("$.trackingNumber").value("AWB12345"));
		move(kovai, kovaiPackage, "deliver", null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));
		assertThat(jdbc.queryForList("SELECT status FROM package_events WHERE package_id = ? ORDER BY id", String.class, kovaiPackage))
			.containsExactly("PLACED", "PACKED", "SHIPPED", "DELIVERED");
	}

	@Test
	void shippingNeedsATrackingNumber() throws Exception {
		move(kovai, kovaiPackage, "pack", null);
		move(kovai, kovaiPackage, "ship", "{\"trackingNumber\":\"\"}").andExpect(status().isBadRequest());
		move(kovai, kovaiPackage, "ship", "{\"trackingNumber\":\"AB 1\"}").andExpect(status().isBadRequest());
		assertThat(packageStatus(kovaiPackage)).isEqualTo("PACKED");
	}

	@Test
	void skippingAStepIsRefused() throws Exception {
		move(kovai, kovaiPackage, "ship", "{\"trackingNumber\":\"AWB12345\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A PLACED package cannot become SHIPPED."));
	}

	/** Review focus: another shop's package id in the URL -> 403, nothing changes. */
	@Test
	void otherShopsPackageIsForbidden() throws Exception {
		move(kovai, chennaiPackage, "pack", null).andExpect(status().isForbidden());
		mvc.perform(post("/api/orders/" + number + "/packages/" + chennaiPackage + "/cancel")
				.cookie(Logins.as(mvc, "kavya")).with(csrf()))
			.andExpect(status().isNotFound());
		assertThat(packageStatus(chennaiPackage)).isEqualTo("PLACED");
	}

	@Test
	void customersCannotUseTheSellerUrls() throws Exception {
		move(ravi, kovaiPackage, "pack", null).andExpect(status().isForbidden());
	}

	@Test
	void sellerCancelNeedsAReasonThenRefundsAndReturnsStock() throws Exception {
		int stock = t.stock(kovaiVariant);
		BigDecimal total = jdbc.queryForObject("SELECT total FROM packages WHERE id = ?", BigDecimal.class, kovaiPackage);

		move(kovai, kovaiPackage, "cancel", "{\"reason\":\"\"}").andExpect(status().isBadRequest());
		move(kovai, kovaiPackage, "cancel", "{\"reason\":\"Size UK 8 is damaged\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));

		assertThat(t.stock(kovaiVariant)).isEqualTo(stock + 2);
		assertThat(jdbc.queryForObject("SELECT refunded_amount FROM payments", BigDecimal.class)).isEqualByComparingTo(total);
		assertThat(jdbc.queryForObject("SELECT status FROM payments", String.class)).isEqualTo("PAID"); // partly refunded
		assertThat(packageStatus(chennaiPackage)).isEqualTo("PLACED"); // the other shop's box is untouched
	}

	@Test
	void cannotCancelAfterShipping() throws Exception {
		move(kovai, kovaiPackage, "pack", null);
		move(kovai, kovaiPackage, "ship", "{\"trackingNumber\":\"AWB12345\"}");
		mvc.perform(post("/api/orders/" + number + "/packages/" + kovaiPackage + "/cancel").cookie(ravi).with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail", containsString("SHIPPED")));
	}

	/** Review focus: cancelling both boxes refunds both, never more than was paid. */
	@Test
	void twoCancelsRefundBothPackages() throws Exception {
		BigDecimal paid = jdbc.queryForObject("SELECT amount FROM payments", BigDecimal.class);
		for (long id : List.of(kovaiPackage, chennaiPackage)) {
			mvc.perform(post("/api/orders/" + number + "/packages/" + id + "/cancel").cookie(ravi).with(csrf()))
				.andExpect(status().isOk());
		}
		assertThat(jdbc.queryForObject("SELECT refunded_amount FROM payments", BigDecimal.class)).isEqualByComparingTo(paid);
		assertThat(jdbc.queryForObject("SELECT status FROM payments", String.class)).isEqualTo("REFUNDED");
		// cancelling again changes nothing
		mvc.perform(post("/api/orders/" + number + "/packages/" + kovaiPackage + "/cancel").cookie(ravi).with(csrf()))
			.andExpect(status().isConflict());
	}

	/** Phase 5 "done when": the customer cancels at the same moment the seller ships -> one wins, the other 409. */
	@Test
	void cancelVsShipRace() throws Exception {
		move(kovai, kovaiPackage, "pack", null);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future<Integer> ship = pool.submit(() -> {
			start.await();
			return move(kovai, kovaiPackage, "ship", "{\"trackingNumber\":\"AWB99999\"}").andReturn().getResponse().getStatus();
		});
		Future<Integer> cancel = pool.submit(() -> {
			start.await();
			return mvc.perform(post("/api/orders/" + number + "/packages/" + kovaiPackage + "/cancel").cookie(ravi).with(csrf()))
				.andReturn().getResponse().getStatus();
		});
		start.countDown();
		List<Integer> answers = List.of(ship.get(), cancel.get());
		pool.shutdown();

		assertThat(answers).containsExactlyInAnyOrder(200, 409);
		String finalStatus = packageStatus(kovaiPackage);
		BigDecimal refunded = jdbc.queryForObject("SELECT refunded_amount FROM payments", BigDecimal.class);
		if (finalStatus.equals("SHIPPED")) {
			assertThat(refunded).isEqualByComparingTo("0"); // the loser never refunded
		}
		else {
			assertThat(finalStatus).isEqualTo("CANCELLED");
			assertThat(refunded.signum()).isPositive();
		}
	}

}

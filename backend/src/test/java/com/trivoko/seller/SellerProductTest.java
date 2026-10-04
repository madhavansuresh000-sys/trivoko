package com.trivoko.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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
import com.trivoko.support.SellerProducts;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

/** Kovai Sports creates, edits and submits its own products (spec section 5, decisions P2-10, P2-11). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerProductTest {

	private static final String URL = "/api/seller/products";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	@Autowired
	private AuditService audit;

	private Cookie kovai;

	private long subCategory;

	@BeforeEach
	void setUp() throws Exception {
		kovai = Logins.as(mvc, "kovai.sports");
		subCategory = SellerProducts.subCategoryId(jdbc);
	}

	private ResultActions create(String json) throws Exception {
		return mvc.perform(post(URL).with(csrf()).cookie(kovai).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions update(long id, String json) throws Exception {
		return mvc.perform(put(URL + "/" + id).with(csrf()).cookie(kovai).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void newProductIsADraftWithItsCheapestPrice() throws Exception {
		String json = """
				{"name":"Kovai Pro Cricket Bat","categoryId":%d,"brand":"Kovai","description":"English willow bat",
				 "variants":[{"label":"Size 5","size":"5","price":2499,"mrp":2999,"stock":10},
				             {"label":"Size 6","size":"6","price":2199,"mrp":2599,"stock":4}]}""".formatted(subCategory);
		create(json)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.slug").value("kovai-pro-cricket-bat"))
			.andExpect(jsonPath("$.priceFrom").value(2199))
			.andExpect(jsonPath("$.variants.length()").value(2))
			.andExpect(jsonPath("$.variants[0].sku").isString());
	}

	@Test
	void mrpBelowPriceIsRejected() throws Exception {
		create(SellerProducts.json(subCategory, "Cheap MRP", "500.00", "400.00", 1)).andExpect(status().isBadRequest());
	}

	@Test
	void moreThanTenVariantsIsRejected() throws Exception {
		StringBuilder variants = new StringBuilder();
		for (int i = 1; i <= 11; i++) {
			variants.append(i > 1 ? "," : "").append("{\"label\":\"V").append(i).append("\",\"price\":100,\"mrp\":120,\"stock\":1}");
		}
		create("{\"name\":\"Too many\",\"categoryId\":" + subCategory + ",\"brand\":\"X\",\"description\":\"Many variants\","
				+ "\"variants\":[" + variants + "]}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.variants").exists());
	}

	@Test
	void topLevelCategoryIsRejected() throws Exception {
		long top = jdbc.queryForObject("SELECT MIN(id) FROM categories WHERE parent_id IS NULL", Long.class);
		create(SellerProducts.json(top, "Wrong shelf", "100.00", "120.00", 1)).andExpect(status().isBadRequest());
	}

	@Test
	void draftCanBeFullyEditedAndSubmitted() throws Exception {
		long id = SellerProducts.create(mvc, kovai, SellerProducts.json(subCategory, "First name", "100.00", "120.00", 1));
		update(id, SellerProducts.json(subCategory, "Better name", "90.00", "120.00", 3))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Better name"))
			.andExpect(jsonPath("$.priceFrom").value(90.0));
		mvc.perform(post(URL + "/" + id + "/submit").with(csrf()).cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void pendingProductCannotBeEdited() throws Exception {
		long id = SellerProducts.create(mvc, kovai, SellerProducts.json(subCategory, "Waiting", "100.00", "120.00", 1));
		mvc.perform(post(URL + "/" + id + "/submit").with(csrf()).cookie(kovai)).andExpect(status().isOk());
		update(id, SellerProducts.json(subCategory, "Changed while waiting", "100.00", "120.00", 1))
			.andExpect(status().isConflict());
	}

	/** Review focus: a live product keeps its name; sending the SAME name with a new price is fine. */
	@Test
	void liveProductCanChangePriceAndStock() throws Exception {
		Map<String, Object> live = liveKovaiProduct();
		long id = ((Number) live.get("id")).longValue();
		long variantId = ((Number) live.get("variant_id")).longValue();

		String json = liveJson(live, (String) live.get("name"), "1.00", ((Number) live.get("mrp")).toString(), 42);
		update(id, json).andExpect(status().isOk())
			.andExpect(jsonPath("$.variants[?(@.id == " + variantId + ")].stock").value(org.hamcrest.Matchers.contains(42)));

		em.flush();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM price_history WHERE variant_id = ? AND new_price = 1.00",
				Integer.class, variantId)).isEqualTo(1);
		assertThat(audit.latestFor("VARIANT", variantId)).extracting(r -> r.getAction()).contains("PRICE_CHANGED");
	}

	@Test
	void liveProductCannotBeRenamed() throws Exception {
		Map<String, Object> live = liveKovaiProduct();
		long id = ((Number) live.get("id")).longValue();
		String json = liveJson(live, "Secretly renamed", ((Number) live.get("price")).toString(),
				((Number) live.get("mrp")).toString(), 5);
		update(id, json).andExpect(status().isConflict());
	}

	@Test
	void myListCanBeFilteredByStatus() throws Exception {
		SellerProducts.create(mvc, kovai, SellerProducts.json(subCategory, "Draft one", "100.00", "120.00", 1));
		mvc.perform(get(URL).param("status", "DRAFT").cookie(kovai))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("DRAFT"))))
			.andExpect(jsonPath("$.content[*].name").value(org.hamcrest.Matchers.hasItem("Draft one")));
	}

	/** A live Kovai Sports product that has exactly one variant (keeps the JSON simple). */
	private Map<String, Object> liveKovaiProduct() {
		return jdbc.queryForMap("""
				SELECT p.id, p.name, p.brand, p.description, p.category_id, v.id AS variant_id, v.label, v.size, v.colour,
				       v.price, v.mrp
				FROM products p JOIN product_variants v ON v.product_id = p.id
				WHERE p.seller_id = 2 AND p.status = 'ACTIVE'
				  AND (SELECT COUNT(*) FROM product_variants x WHERE x.product_id = p.id) = 1
				ORDER BY p.id LIMIT 1""");
	}

	private static String liveJson(Map<String, Object> live, String name, String price, String mrp, int stock) {
		return """
				{"name":"%s","categoryId":%s,"brand":"%s","description":"%s",
				 "variants":[{"id":%s,"label":"%s","size":%s,"colour":%s,"price":%s,"mrp":%s,"stock":%d}]}"""
			.formatted(name, live.get("category_id"), live.get("brand"), live.get("description"), live.get("variant_id"),
					live.get("label"), quoted(live.get("size")), quoted(live.get("colour")), price, mrp, stock);
	}

	private static String quoted(Object value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

}

package com.trivoko.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The public catalogue URLs, called WITHOUT logging in, against the Flyway sample data (V2).
 * Numbers come from tools/seed-gen/make_seed.py.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

	@Autowired
	private MockMvc mvc;

	// ---------- GET /api/products ----------

	/** Phase 1 "done when" check: ?category=phones&sort=price returns the correct pages. */
	@Test
	void phonesSortedByPriceComeInTheRightPages() throws Exception {
		mvc.perform(get("/api/products").param("category", "phones").param("sort", "price").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(4))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.content[*].name", contains("Kaveri K5", "Volta V12 5G")))
			.andExpect(jsonPath("$.content[0].priceFrom").value(8999.00));

		mvc.perform(get("/api/products").param("category", "phones").param("sort", "price").param("size", "2")
				.param("page", "1"))
			.andExpect(jsonPath("$.content[*].name", contains("Nimbus N8 Pro", "Orbit X Ultra")))
			.andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void topCategoryIncludesItsSubCategories() throws Exception {
		mvc.perform(get("/api/products").param("category", "mobiles-accessories"))
			.andExpect(jsonPath("$.totalElements").value(12));
	}

	@Test
	void onlyActiveProductsOfApprovedSellersAreListed() throws Exception {
		mvc.perform(get("/api/products").param("size", "48"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(116))
			.andExpect(jsonPath("$.content[*].slug", not(hasItem("arc-soundbar-2-1"))));
	}

	@Test
	void newestProductsComeFirstByDefault() throws Exception {
		mvc.perform(get("/api/products"))
			.andExpect(jsonPath("$.size").value(24))
			.andExpect(jsonPath("$.content[0].name").value("PlayBox lunch bag"));
	}

	@Test
	void filterByBrand() throws Exception {
		mvc.perform(get("/api/products").param("brand", "Arc").param("size", "48"))
			.andExpect(jsonPath("$.totalElements").value(11))
			.andExpect(jsonPath("$.content[*].brand", everyItem(is("Arc"))));
	}

	@Test
	void filterBySeveralBrands() throws Exception {
		// Volta: V12 5G, flip cover, 33W charger, car charger, pen drive (5) + Nimbus: N8 Pro, power bank (2)
		mvc.perform(get("/api/products").param("brand", "Volta", "Nimbus"))
			.andExpect(jsonPath("$.totalElements").value(7));
	}

	@Test
	void filterByPriceRange() throws Exception {
		mvc.perform(get("/api/products").param("minPrice", "500").param("maxPrice", "1000").param("size", "48"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].priceFrom", everyItem(greaterThanOrEqualTo(500.0))))
			.andExpect(jsonPath("$.content[*].priceFrom", everyItem(lessThanOrEqualTo(1000.0))));
	}

	@Test
	void inStockHidesSoldOutProducts() throws Exception {
		mvc.perform(get("/api/products").param("category", "cycling"))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.content[*].slug", hasItem("velo-bicycle-lights-set")));
		mvc.perform(get("/api/products").param("category", "cycling").param("inStock", "true"))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[*].slug", not(hasItem("velo-bicycle-lights-set"))));
	}

	@Test
	void filterBySeller() throws Exception {
		mvc.perform(get("/api/products").param("seller", "pondy-books"))
			.andExpect(jsonPath("$.totalElements").value(12))
			.andExpect(jsonPath("$.content[*].sellerName", everyItem(is("Pondy Books"))));
	}

	@Test
	void mostExpensiveFirst() throws Exception {
		mvc.perform(get("/api/products").param("sort", "price").param("dir", "desc"))
			.andExpect(jsonPath("$.content[0].name").value("Zentra Pro 16"));
	}

	@Test
	void cardShowsDiscountAndSeller() throws Exception {
		mvc.perform(get("/api/products").param("category", "phones").param("sort", "price"))
			.andExpect(jsonPath("$.content[0].slug").value("kaveri-k5"))
			.andExpect(jsonPath("$.content[0].sellerSlug").value("chennai-mobiles"))
			.andExpect(jsonPath("$.content[0].discountPercent", greaterThanOrEqualTo(9)))
			.andExpect(jsonPath("$.content[0].inStock").isBoolean());
	}

	@Test
	void pageSizeIsLimitedTo48() throws Exception {
		mvc.perform(get("/api/products").param("size", "500"))
			.andExpect(jsonPath("$.size").value(48));
	}

	@Test
	void unknownSortIsABadRequest() throws Exception {
		mvc.perform(get("/api/products").param("sort", "popular"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void minPriceAboveMaxPriceIsABadRequest() throws Exception {
		mvc.perform(get("/api/products").param("minPrice", "1000").param("maxPrice", "500"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void negativePageIsABadRequest() throws Exception {
		mvc.perform(get("/api/products").param("page", "-1"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unknownCategoryIsNotFound() throws Exception {
		mvc.perform(get("/api/products").param("category", "spaceships"))
			.andExpect(status().isNotFound());
	}

	// ---------- GET /api/products/{slug} ----------

	@Test
	void productDetailsWithVariantsSellerAndCategory() throws Exception {
		mvc.perform(get("/api/products/silicone-case-for-iphone-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Silicone case for iPhone 15"))
			.andExpect(jsonPath("$.variants.length()").value(3))
			.andExpect(jsonPath("$.variants[0].label").value("Black"))
			.andExpect(jsonPath("$.variants[0].inStock").value(true))
			.andExpect(jsonPath("$.seller.slug").value("chennai-mobiles"))
			.andExpect(jsonPath("$.seller.city").value("Chennai"))
			.andExpect(jsonPath("$.category.slug").value("cases-covers"))
			.andExpect(jsonPath("$.category.parent.slug").value("mobiles-accessories"))
			.andExpect(jsonPath("$.images").isArray());
	}

	@Test
	void lowStockShowsHowManyAreLeft() throws Exception {
		mvc.perform(get("/api/products/salem-steel-idli-maker"))
			.andExpect(jsonPath("$.variants[0].onlyLeft").value(3));
		mvc.perform(get("/api/products/silicone-case-for-iphone-15"))
			.andExpect(jsonPath("$.variants[0].onlyLeft").doesNotExist());
	}

	@Test
	void pendingProductIsNotFound() throws Exception {
		mvc.perform(get("/api/products/arc-soundbar-2-1"))
			.andExpect(status().isNotFound());
	}

	@Test
	void unknownProductIsNotFound() throws Exception {
		mvc.perform(get("/api/products/no-such-thing"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Product no-such-thing not found"));
	}

	// ---------- GET /api/categories, /api/sellers/{slug} ----------

	@Test
	void categoryTree() throws Exception {
		mvc.perform(get("/api/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(10))
			.andExpect(jsonPath("$[0].name").value("Mobiles & Accessories"))
			.andExpect(jsonPath("$[0].children[*].slug", contains("phones", "cases-covers", "chargers-cables")));
	}

	@Test
	void sellerShopPage() throws Exception {
		mvc.perform(get("/api/sellers/chennai-mobiles"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shopName").value("Chennai Mobiles"))
			.andExpect(jsonPath("$.city").value("Chennai"))
			.andExpect(jsonPath("$.status").doesNotExist());
	}

	@Test
	void pendingSellerIsNotFound() throws Exception {
		mvc.perform(get("/api/sellers/erode-organics"))
			.andExpect(status().isNotFound());
	}

}

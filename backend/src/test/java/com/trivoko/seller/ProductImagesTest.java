package com.trivoko.seller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/**
 * PUT /api/seller/products/{id}/images: the seller's photo list (max 8, in order; the first is the main photo).
 * Photos are uploaded straight to Cloudinary by the browser; only OUR Cloudinary folder's URLs are accepted.
 */
@SpringBootTest(properties = "app.cloudinary.cloud-name=trivoko-test")
@AutoConfigureMockMvc
@Transactional
class ProductImagesTest {

	private static final String OURS = "https://res.cloudinary.com/trivoko-test/image/upload/v1/trivoko/products/";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private Cookie kovai;

	private long product;

	private String slug;

	@BeforeEach
	void aKovaiProduct() throws Exception {
		kovai = Logins.as(mvc, "kovai.sports");
		product = jdbc.queryForObject("SELECT MIN(id) FROM products WHERE seller_id = 2 AND status = 'ACTIVE'", Long.class);
		slug = jdbc.queryForObject("SELECT slug FROM products WHERE id = ?", String.class, product);
	}

	private ResultActions save(Cookie who, long id, String json) throws Exception {
		return mvc.perform(put("/api/seller/products/" + id + "/images").cookie(who).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String photos(int count) {
		return IntStream.range(0, count)
			.mapToObj(i -> "{\"url\":\"" + OURS + "shoe-" + i + ".jpg\",\"altText\":\"Side " + i + "\"}")
			.collect(Collectors.joining(",", "[", "]"));
	}

	@Test
	void photosAreSavedInOrderAndTheFirstIsTheCardPhoto() throws Exception {
		save(kovai, product, photos(2)).andExpect(status().isOk());
		mvc.perform(get("/api/products/" + slug))
			.andExpect(jsonPath("$.images.length()").value(2))
			.andExpect(jsonPath("$.images[0].url").value(OURS + "shoe-0.jpg"))
			.andExpect(jsonPath("$.images[1].altText").value("Side 1"));
		// moving a photo up = sending the list in the new order
		save(kovai, product, "[{\"url\":\"" + OURS + "shoe-1.jpg\"},{\"url\":\"" + OURS + "shoe-0.jpg\"}]");
		mvc.perform(get("/api/products/" + slug)).andExpect(jsonPath("$.images[0].url").value(OURS + "shoe-1.jpg"));
	}

	@Test
	void atMostEightPhotos() throws Exception {
		save(kovai, product, photos(9)).andExpect(status().isBadRequest());
	}

	/** Review focus: a photo from another website is refused (only our own Cloudinary folder). */
	@Test
	void foreignPhotoUrlIsRefused() throws Exception {
		save(kovai, product, "[{\"url\":\"https://evil.example/x.jpg\"}]")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("TriVoKo photo")));
		save(kovai, product, "[{\"url\":\"https://res.cloudinary.com/someone-else/image/upload/x.jpg\"}]")
			.andExpect(status().isBadRequest());
	}

	@Test
	void anotherShopsProductIsForbidden() throws Exception {
		long chennaiProduct = jdbc.queryForObject("SELECT MIN(id) FROM products WHERE seller_id = 1", Long.class);
		save(kovai, chennaiProduct, photos(1)).andExpect(status().isForbidden());
	}

	@Test
	void anEmptyListRemovesEveryPhoto() throws Exception {
		save(kovai, product, photos(1));
		save(kovai, product, "[]").andExpect(status().isOk());
		mvc.perform(get("/api/products/" + slug)).andExpect(jsonPath("$.images.length()").value(0));
	}

}

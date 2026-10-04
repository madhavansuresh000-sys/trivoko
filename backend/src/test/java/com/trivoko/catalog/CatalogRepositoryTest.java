package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.seller.Seller;
import com.trivoko.seller.SellerRepository;
import com.trivoko.seller.SellerStatus;

import jakarta.persistence.EntityManager;

/** The Flyway tables and the JPA entities fit together. Every test is rolled back. */
@SpringBootTest
@Transactional
class CatalogRepositoryTest {

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private SellerRepository sellers;

	@Autowired
	private ProductRepository products;

	@Autowired
	private EntityManager em;

	@Test
	void productIsSavedWithVariantsAndReadBack() {
		Product product = newProduct("test-phone-x");
		product.addVariant(variant("TEST-X-128", "Black / 128 GB", "12999.00", "15999.00", 10));
		product.addVariant(variant("TEST-X-256", "Black / 256 GB", "14999.00", "17999.00", 0));
		products.save(product);
		em.flush();
		em.clear();

		Product found = products.findBySlug("test-phone-x").orElseThrow();
		assertThat(found.getVariants()).hasSize(2);
		assertThat(found.getVariants().getFirst().getPrice()).isEqualByComparingTo("12999.00");
		assertThat(found.getVariants().getFirst().getVersion()).isZero();
		assertThat(found.getSeller().getShopName()).isEqualTo("Test Shop");
		assertThat(found.getCategory().getParent().getSlug()).isEqualTo("test-top");
		assertThat(found.getStatus()).isEqualTo(ProductStatus.DRAFT);
	}

	@Test
	void databaseRejectsNegativeStock() {
		Product product = newProduct("test-bad-stock");
		product.addVariant(variant("TEST-BAD", "One size", "100.00", "100.00", -1));
		// IDENTITY ids: save() sends the INSERT at once, so the database check fires inside save()
		assertThatThrownBy(() -> products.saveAndFlush(product))
			.isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
	}

	@Test
	void databaseRejectsPriceAboveMrp() {
		Product product = newProduct("test-bad-mrp");
		product.addVariant(variant("TEST-MRP", "One size", "500.00", "400.00", 1));
		// IDENTITY ids: save() sends the INSERT at once, so the database check fires inside save()
		assertThatThrownBy(() -> products.saveAndFlush(product))
			.isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
	}

	private Product newProduct(String slug) {
		Category top = categories.save(new Category("Test top", "test-top", null, 99));
		Category sub = categories.save(new Category("Test sub", "test-sub-" + slug, top, 1));
		Seller seller = sellers.save(new Seller("Test Shop", "test-shop-" + slug, "Chennai", "A test shop", SellerStatus.APPROVED));
		return new Product(seller, sub, "Test product " + slug, slug, "Volta", "Description");
	}

	private static ProductVariant variant(String sku, String label, String price, String mrp, int stock) {
		return new ProductVariant(sku, label, null, null, new BigDecimal(price), new BigDecimal(mrp), stock);
	}

}

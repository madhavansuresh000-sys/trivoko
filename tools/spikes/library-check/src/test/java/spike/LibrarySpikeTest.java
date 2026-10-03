package spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.cloudinary.Cloudinary;
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import com.stripe.Stripe;
import com.stripe.param.checkout.SessionCreateParams;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import jakarta.persistence.EntityManager;

@SpringBootTest
class LibrarySpikeTest {

	@Autowired
	EntityManager em;

	@Autowired
	TransactionTemplate tx;

	@Test
	void hibernateSearchFuzzyAndFacets() {
		tx.executeWithoutResult(s -> {
			em.persist(new Product("Apple iPhone 15 silicone case", "Apple"));
			em.persist(new Product("Samsung Galaxy S24 phone", "Samsung"));
			em.persist(new Product("Kovai running shoes", "Kovai"));
		});
		List<String> hits = tx.execute(s -> Search.session(em).search(Product.class)
			.where(f -> f.match().field("title").matching("iphnoe").fuzzy(2))
			.fetchHits(10).stream().map(Product::getTitle).toList());
		System.out.println("SPIKE fuzzy hits: " + hits);
		assertThat(hits.get(0)).isEqualTo("Apple iPhone 15 silicone case");

		AggregationKey<Map<String, Long>> brands = AggregationKey.of("brands");
		Map<String, Long> facets = tx.execute(s -> Search.session(em).search(Product.class)
			.where(f -> f.matchAll())
			.aggregation(brands, f -> f.terms().field("brand", String.class))
			.fetch(0).aggregation(brands));
		System.out.println("SPIKE facets: " + facets);
		assertThat(facets).containsEntry("Samsung", 1L).hasSize(3);
	}

	@Test
	void stripeBuildsACheckoutSessionRequest() {
		Stripe.apiKey = "sk_test_dummy";
		SessionCreateParams p = SessionCreateParams.builder()
			.setMode(SessionCreateParams.Mode.PAYMENT)
			.setSuccessUrl("http://localhost:5173/ok")
			.addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
				.setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency("inr").setUnitAmount(1540000L)
					.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("Order TV-1001").build())
					.build())
				.build())
			.build();
		System.out.println("SPIKE stripe api version: " + Stripe.API_VERSION);
		assertThat(p.getLineItems()).hasSize(1);
	}

	@Test
	void cloudinarySignsAnUpload() {
		Cloudinary c = new Cloudinary(Map.of("cloud_name", "demo", "api_key", "123", "api_secret", "abc"));
		Map<String, Object> params = new java.util.HashMap<>(Map.of("timestamp", 1_700_000_000L, "folder", "products"));
		String sig = c.apiSignRequest(params, "abc", 2);
		System.out.println("SPIKE cloudinary signature: " + sig);
		assertThat(sig).hasSize(40);
	}

	@Test
	void openPdfWritesAnInvoice() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document doc = new Document();
		PdfWriter.getInstance(doc, out);
		doc.open();
		doc.add(new Paragraph("TriVoKo invoice TV-1001"));
		doc.close();
		System.out.println("SPIKE pdf bytes: " + out.size());
		assertThat(new String(out.toByteArray(), 0, 5)).isEqualTo("%PDF-");
	}

	@Test
	void archUnitReadsOurClasses() {
		var classes = new ClassFileImporter().importPackages("spike");
		assertThat(classes.contain(Product.class)).isTrue();
	}

}

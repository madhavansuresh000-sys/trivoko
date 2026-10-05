package com.trivoko.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.trivoko.support.Logins;

import jakarta.servlet.http.Cookie;

/** GET /api/orders/{number}/invoice.pdf - only for my own PAID orders. */
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private CheckoutTestSupport t;

	private Cookie ravi;

	private String number;

	private String session;

	private String productName;

	@BeforeEach
	void ravisOrder() throws Exception {
		t = new CheckoutTestSupport(mvc, jdbc);
		ravi = Logins.as(mvc, "ravi");
		long address = t.addressOf(2);
		long variant = t.variantOf(1);
		productName = jdbc.queryForObject("""
				SELECT p.name FROM products p JOIN product_variants v ON v.product_id = p.id WHERE v.id = ?""",
				String.class, variant);
		t.addToCart(ravi, variant, 2);
		String json = t.place(ravi, address, null, t.previewTotal(ravi, address, null))
			.andReturn().getResponse().getContentAsString();
		number = JsonPath.read(json, "$.number");
		String url = JsonPath.read(json, "$.redirectUrl");
		session = url.substring(url.lastIndexOf('/') + 1);
	}

	@AfterEach
	void clean() {
		jdbc.update("DELETE FROM notifications");
		t.clean();
	}

	private String pdfText(byte[] pdf) throws Exception {
		PdfReader reader = new PdfReader(pdf);
		StringBuilder text = new StringBuilder();
		PdfTextExtractor extractor = new PdfTextExtractor(reader);
		for (int page = 1; page <= reader.getNumberOfPages(); page++) {
			text.append(extractor.getTextFromPage(page)).append('\n');
		}
		return text.toString();
	}

	@Test
	void paidOrderHasAnInvoiceWithItsLinesAndTotal() throws Exception {
		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(ravi).with(csrf()));
		BigDecimal total = jdbc.queryForObject("SELECT grand_total FROM orders WHERE number = ?", BigDecimal.class, number);

		byte[] pdf = mvc.perform(get("/api/orders/" + number + "/invoice.pdf").cookie(ravi))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(header().string("Content-Disposition", Matchers.containsString("TriVoKo-invoice-" + number + ".pdf")))
			.andReturn().getResponse().getContentAsByteArray();

		assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
		String text = pdfText(pdf);
		assertThat(text).contains("Invoice", number, productName, "Chennai Mobiles", "Ravi");
		assertThat(text).contains(total.toPlainString());
	}

	@Test
	void unpaidOrderHasNoInvoiceYet() throws Exception {
		mvc.perform(get("/api/orders/" + number + "/invoice.pdf").cookie(ravi)).andExpect(status().isConflict());
	}

	@Test
	void someoneElsesInvoiceIsNotFound() throws Exception {
		mvc.perform(post("/api/payments/fake/" + session + "/complete").cookie(ravi).with(csrf()));
		mvc.perform(get("/api/orders/" + number + "/invoice.pdf").cookie(Logins.as(mvc, "kavya")))
			.andExpect(status().isNotFound());
	}

}

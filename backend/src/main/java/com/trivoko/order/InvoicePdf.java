package com.trivoko.order;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import com.trivoko.common.Money;

/**
 * The PDF invoice of a PAID order (OpenPDF 3 - package org.openpdf.text, see docs/decisions.md D1).
 * One table per package (= per seller), then the totals. Amounts are written "Rs. 1234.50": the built-in PDF
 * fonts have no rupee sign.
 */
final class InvoicePdf {

	private static final Color TEAL = new Color(15, 118, 110); // brand-700

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

	private InvoicePdf() {
	}

	static byte[] of(Order order, String customerName) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
		PdfWriter.getInstance(doc, out);
		doc.open();

		Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, TEAL);
		Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
		Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
		Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

		doc.add(new Paragraph("TriVoKo - Invoice", title));
		doc.add(new Paragraph("Order " + order.getNumber() + "   |   Paid "
				+ (order.getPaidAt() == null ? "" : order.getPaidAt().format(WHEN)), normal));
		doc.add(new Paragraph(" "));

		Order.ShipTo to = order.shipTo();
		doc.add(new Paragraph("Bill and ship to", bold));
		doc.add(new Paragraph(customerName + " (" + to.name() + ")", normal));
		doc.add(new Paragraph(to.line1() + (to.line2() == null ? "" : ", " + to.line2()), normal));
		doc.add(new Paragraph(to.city() + ", " + to.state() + " " + to.pincode() + "   |   Phone " + to.phone(), normal));
		doc.add(new Paragraph(" "));

		int n = 1;
		for (OrderPackage p : order.getPackages()) {
			doc.add(new Paragraph("Package " + n++ + " - sold and shipped by " + p.getSellerName(), bold));
			PdfPTable table = new PdfPTable(new float[] { 5, 1.2f, 2, 2, 2 });
			table.setWidthPercentage(100);
			table.setSpacingBefore(4);
			for (String h : new String[] { "Item", "Qty", "Price", "Coupon", "Amount" }) {
				PdfPCell cell = new PdfPCell(new Phrase(h, bold));
				cell.setBackgroundColor(new Color(240, 253, 250)); // brand-50
				table.addCell(cell);
			}
			for (OrderItem i : p.getItems()) {
				table.addCell(new Phrase(i.getProductName() + " (" + i.getVariantLabel() + ")", normal));
				table.addCell(right(String.valueOf(i.getQuantity()), normal));
				table.addCell(right(rs(i.getUnitPrice()), normal));
				table.addCell(right(i.getDiscountShare().signum() == 0 ? "-" : "- " + rs(i.getDiscountShare()), normal));
				table.addCell(right(rs(i.getLineTotal().subtract(i.getDiscountShare())), normal));
			}
			doc.add(table);
			doc.add(right("Delivery " + (p.getShippingFee().signum() == 0 ? "free" : rs(p.getShippingFee()))
					+ "   |   Package total " + rs(p.getTotal()), normal));
			doc.add(new Paragraph(" "));
		}

		PdfPTable totals = new PdfPTable(new float[] { 3, 2 });
		totals.setWidthPercentage(45);
		totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
		row(totals, "Items", rs(order.getItemsTotal()), normal);
		if (order.getDiscountTotal().signum() > 0) {
			row(totals, "Coupon " + order.getCouponCode(), "- " + rs(order.getDiscountTotal()), normal);
		}
		row(totals, "Delivery", order.getShippingTotal().signum() == 0 ? "free" : rs(order.getShippingTotal()), normal);
		row(totals, "Total paid", rs(order.getGrandTotal()), bold);
		doc.add(totals);

		doc.add(new Paragraph(" "));
		doc.add(new Paragraph("One payment, split into one package per seller. Prices include all taxes. "
				+ "This is a demo shop (portfolio project) - no real goods are sold.", small));
		doc.close();
		return out.toByteArray();
	}

	/** "Rs. 1234.50" (2 decimals, no grouping - the PDF fonts have no rupee sign). */
	private static String rs(BigDecimal amount) {
		return "Rs. " + Money.round(amount).toPlainString();
	}

	private static PdfPCell right(String text, Font font) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		return cell;
	}

	private static void row(PdfPTable table, String label, String value, Font font) {
		PdfPCell left = new PdfPCell(new Phrase(label, font));
		left.setBorder(0);
		table.addCell(left);
		PdfPCell r = right(value, font);
		r.setBorder(0);
		table.addCell(r);
	}

}

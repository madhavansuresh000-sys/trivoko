package com.trivoko.order;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One line of a bill, frozen at the moment of ordering: name, variant and price are COPIES, so a later
 * price change never changes an old order. discountShare = this line's part of the coupon (Phase 8 refunds
 * exactly lineTotal - discountShare).
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "package_id")
	private OrderPackage orderPackage;

	@Column(name = "variant_id", nullable = false)
	private Long variantId;

	@Column(name = "product_slug", nullable = false, length = 220)
	private String productSlug;

	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	@Column(name = "variant_label", nullable = false, length = 100)
	private String variantLabel;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal mrp;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	@Column(name = "discount_share", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountShare;

	public OrderItem(Long variantId, String productSlug, String productName, String variantLabel, BigDecimal unitPrice,
			BigDecimal mrp, int quantity, BigDecimal lineTotal, BigDecimal discountShare) {
		this.variantId = variantId;
		this.productSlug = productSlug;
		this.productName = productName;
		this.variantLabel = variantLabel;
		this.unitPrice = unitPrice;
		this.mrp = mrp;
		this.quantity = quantity;
		this.lineTotal = lineTotal;
		this.discountShare = discountShare;
	}

	void setOrderPackage(OrderPackage orderPackage) {
		this.orderPackage = orderPackage;
	}

}

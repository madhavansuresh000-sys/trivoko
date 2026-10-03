package com.trivoko.catalog;

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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One buyable version of a product, e.g. "Black / 128 GB" or "UK 8".
 * Price and stock live HERE: a size 8 shoe can be sold out while size 9 is in stock.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false, unique = true, length = 60)
	private String sku;

	/** What the customer sees on the button, e.g. "Black / 128 GB". */
	@Column(nullable = false, length = 100)
	private String label;

	@Column(length = 20)
	private String size;

	@Column(length = 40)
	private String colour;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	/** Maximum retail price (the crossed-out price). Never below price. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal mrp;

	@Column(nullable = false)
	private int stock;

	/** Optimistic lock: if two people save this row at the same moment, the second gets 409. */
	@Version
	@Column(nullable = false)
	private long version;

	public ProductVariant(String sku, String label, String size, String colour,
			BigDecimal price, BigDecimal mrp, int stock) {
		this.sku = sku;
		this.label = label;
		this.size = size;
		this.colour = colour;
		this.price = price;
		this.mrp = mrp;
		this.stock = stock;
	}

}

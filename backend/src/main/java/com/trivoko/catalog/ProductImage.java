package com.trivoko.catalog;

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
import lombok.Setter;

/** One product photo (a Cloudinary URL). sortOrder 0 = the main photo. */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(name = "alt_text", length = 200)
	private String altText;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public ProductImage(String url, String altText, int sortOrder) {
		this.url = url;
		this.altText = altText;
		this.sortOrder = sortOrder;
	}

}

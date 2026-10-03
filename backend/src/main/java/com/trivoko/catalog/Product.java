package com.trivoko.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.trivoko.seller.Seller;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A product page, e.g. "Volta V12 5G" sold by Chennai Mobiles.
 * The things you actually buy are its variants (Black / 128 GB, Blue / 256 GB ...),
 * because each one has its own price and stock.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id")
	private Seller seller;

	/** Always a sub-category (e.g. "Mobiles"), never a top category. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, unique = true, length = 220)
	private String slug;

	@Column(nullable = false, length = 80)
	private String brand;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status = ProductStatus.DRAFT;

	@Column(name = "rejection_reason", length = 300)
	private String rejectionReason;

	/** Price and MRP of the cheapest variant. Only refreshPriceFrom() changes them. */
	@Setter(AccessLevel.NONE)
	@Column(name = "price_from", precision = 12, scale = 2)
	private BigDecimal priceFrom;

	@Setter(AccessLevel.NONE)
	@Column(name = "mrp_from", precision = 12, scale = 2)
	private BigDecimal mrpFrom;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<ProductVariant> variants = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder")
	private List<ProductImage> images = new ArrayList<>();

	public Product(Seller seller, Category category, String name, String slug, String brand, String description) {
		this.seller = seller;
		this.category = category;
		this.name = name;
		this.slug = slug;
		this.brand = brand;
		this.description = description;
	}

	/** Adds a variant and keeps both sides of the link (and price_from) correct. */
	public void addVariant(ProductVariant variant) {
		variant.setProduct(this);
		variants.add(variant);
		refreshPriceFrom();
	}

	public void addImage(ProductImage image) {
		image.setProduct(this);
		images.add(image);
	}

	/** price_from / mrp_from = the cheapest variant. Call after any variant price changes. */
	public void refreshPriceFrom() {
		variants.stream()
			.min(Comparator.comparing(ProductVariant::getPrice))
			.ifPresentOrElse(cheapest -> {
				priceFrom = cheapest.getPrice();
				mrpFrom = cheapest.getMrp();
			}, () -> {
				priceFrom = null;
				mrpFrom = null;
			});
	}

}

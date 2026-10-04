package com.trivoko.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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

/** "On 3 Oct the price of Black / 128 GB went from 13,999 to 12,999." Written only by ProductService. */
@Entity
@Table(name = "price_history")
@Getter
@NoArgsConstructor
public class PriceHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Column(name = "old_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal oldPrice;

	@Column(name = "new_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal newPrice;

	@CreationTimestamp
	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	public PriceHistory(ProductVariant variant, BigDecimal oldPrice, BigDecimal newPrice) {
		this.variant = variant;
		this.oldPrice = oldPrice;
		this.newPrice = newPrice;
	}

}

package com.trivoko.flashsale;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A variant offered in a sale: price, quantity (already taken out of normal stock) and how many are claimed.
 * "sold" is changed ONLY by the two atomic UPDATEs in FlashItemRepository, never by setting a field here.
 */
@Entity
@Table(name = "flash_sale_items")
@Getter
@NoArgsConstructor
public class FlashSaleItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sale_id", nullable = false)
	private Long saleId;

	@Column(name = "variant_id", nullable = false)
	private Long variantId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal salePrice;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, insertable = false, updatable = false)
	private int sold;

	public FlashSaleItem(Long saleId, Long variantId, Long sellerId, BigDecimal salePrice, int quantity) {
		this.saleId = saleId;
		this.variantId = variantId;
		this.sellerId = sellerId;
		this.salePrice = salePrice;
		this.quantity = quantity;
	}

	public int left() {
		return Math.max(0, quantity - sold);
	}

}

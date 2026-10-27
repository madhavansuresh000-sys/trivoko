package com.trivoko.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One saved product for one customer. Prices are the product's "from" price (the number on the card).
 * The alert job compares today's price with lastAlertedPrice, or priceWhenAdded if we never told them before.
 */
@Entity
@Table(name = "wishlist_items")
@Getter
@NoArgsConstructor
public class WishlistItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(name = "price_when_added", nullable = false, updatable = false, precision = 12, scale = 2)
	private BigDecimal priceWhenAdded;

	@Setter
	@Column(name = "last_alerted_price", precision = 12, scale = 2)
	private BigDecimal lastAlertedPrice;

	/** Set by the PriceChanged listener, cleared by PriceAlertJob. */
	@Setter
	@Column(name = "price_changed", nullable = false)
	private boolean priceChanged;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public WishlistItem(Long userId, Long productId, BigDecimal priceWhenAdded) {
		this.userId = userId;
		this.productId = productId;
		this.priceWhenAdded = priceWhenAdded;
	}

	/** The price the customer last heard from us: the alert must go below this. */
	public BigDecimal referencePrice() {
		return lastAlertedPrice != null ? lastAlertedPrice : priceWhenAdded;
	}

}

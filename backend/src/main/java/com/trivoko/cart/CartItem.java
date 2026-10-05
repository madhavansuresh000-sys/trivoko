package com.trivoko.cart;

import java.time.LocalDateTime;

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

/** One line of a cart: "variant 57 (Kovai Run Pro, Blue / UK 8), quantity 2". */
@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor
public class CartItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id")
	private Cart cart;

	/** The catalogue's variant id (a plain number: the cart module does not map catalogue entities). */
	@Column(name = "variant_id", nullable = false)
	private Long variantId;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "added_at", nullable = false)
	private LocalDateTime addedAt = LocalDateTime.now();

	CartItem(Cart cart, Long variantId, int quantity) {
		this.cart = cart;
		this.variantId = variantId;
		this.quantity = quantity;
	}

	void setQuantity(int quantity) {
		this.quantity = quantity;
		cart.touch();
	}

}

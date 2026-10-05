package com.trivoko.cart;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The saved cart of ONE logged-in user. It holds only "which variant, how many" - the price, the name and
 * the stock are read from the catalogue every time the cart is shown (CartPricing), so they are never stale.
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor
public class Cart {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The user module's id; kept as a plain number (a module never maps another module's entities). */
	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	/** Oldest first: the cart page lists items (and seller packages) in the order they were added. */
	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("addedAt ASC, id ASC")
	private List<CartItem> items = new ArrayList<>();

	public Cart(Long userId) {
		this.userId = userId;
	}

	public Optional<CartItem> line(Long variantId) {
		return items.stream().filter(i -> i.getVariantId().equals(variantId)).findFirst();
	}

	public void add(Long variantId, int quantity) {
		items.add(new CartItem(this, variantId, quantity));
		touch();
	}

	public void remove(Long variantId) {
		if (items.removeIf(i -> i.getVariantId().equals(variantId))) {
			touch();
		}
	}

	void touch() {
		updatedAt = LocalDateTime.now();
	}

}

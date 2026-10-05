package com.trivoko.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

	/** The cart with its lines in one query (the cart page always needs both). */
	@EntityGraph(attributePaths = "items")
	Optional<Cart> findByUserId(Long userId);

}

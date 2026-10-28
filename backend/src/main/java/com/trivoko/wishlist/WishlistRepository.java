package com.trivoko.wishlist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

	Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

	List<WishlistItem> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	long countByUserId(Long userId);

	/** Five price changes in a minute -> the same rows are flagged five times, still ONE flag each. */
	@Modifying
	@Query("update WishlistItem w set w.priceChanged = true where w.productId = :productId")
	int flagProduct(Long productId);

	List<WishlistItem> findByPriceChangedTrue();

}

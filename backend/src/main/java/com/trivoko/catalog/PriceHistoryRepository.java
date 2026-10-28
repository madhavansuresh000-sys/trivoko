package com.trivoko.catalog;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read by Phase 9 (lowest price in 30 days). Written only by ProductService.changePrice. */
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

	/**
	 * Since this time, over all variants of the product: lowest old price, lowest new price, number of changes.
	 * Uses ix_price_history_variant (variant_id, changed_at).
	 */
	@Query("""
			select new com.trivoko.catalog.PriceWindow(min(h.oldPrice), min(h.newPrice), count(h)) from PriceHistory h
			where h.variant.product.id = :productId and h.changedAt >= :since""")
	PriceWindow lowestSince(@Param("productId") Long productId, @Param("since") LocalDateTime since);

}

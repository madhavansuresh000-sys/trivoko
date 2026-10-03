package com.trivoko.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Read by Phase 9 (lowest price in 30 days, price-drop alerts). Written only by ProductService.changePrice. */
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
}

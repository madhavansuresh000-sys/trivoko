package com.trivoko.order;

import java.util.Collection;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * The SQL half of "Customers also bought": which OTHER variants were in the same PAID orders as this product's
 * variants, most shared orders first. Kept for 1 hour (cache "alsoBought", CacheConfig) - buying habits do not
 * change by the minute, and this query reads many order lines. Only these numbers are cached; the cards (with
 * today's price and stock) are built fresh every time. A separate bean so @Cacheable works (Spring's proxy).
 */
@Component
@RequiredArgsConstructor
class AlsoBoughtRanking {

	/** Read a few more than we show: some may belong to products that are no longer live. */
	static final int LOOKUP = 30;

	private final OrderPackageRepository packages;

	@Cacheable(cacheNames = "alsoBought", key = "#productId")
	@Transactional(readOnly = true)
	public List<Long> otherVariants(Long productId, Collection<Long> variantIds) {
		return variantIds.isEmpty() ? List.of()
				: packages.boughtTogether(variantIds, OrderStatus.PAID, PageRequest.of(0, LOOKUP));
	}

}

package com.trivoko.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.WatchedProduct;
import com.trivoko.common.BusinessRuleException;

import lombok.RequiredArgsConstructor;

/**
 * The customer's wishlist (Phase 9 step 3). Saved per PRODUCT at its "from" price - the number on the card - so
 * the price-drop job (step 4) can later say "now Rs X lower than when you saved it".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

	public static final int MAX_ITEMS = 100;

	private final WishlistRepository items;

	private final ProductService productService;

	/** One saved product as the wishlist page shows it. priceDrop = how much cheaper than when saved (0 if not). */
	public record WishlistEntry(ProductCard product, boolean available, BigDecimal priceWhenAdded,
			BigDecimal priceDrop, LocalDateTime addedAt) {
	}

	/** Pressing the heart twice is harmless: the second press changes nothing. */
	@Transactional
	public void add(Long userId, Long productId) {
		if (items.findByUserIdAndProductId(userId, productId).isPresent()) {
			return;
		}
		if (items.countByUserId(userId) >= MAX_ITEMS) {
			throw new BusinessRuleException("Your wishlist is full (" + MAX_ITEMS + " items). Remove something first.");
		}
		ProductCard product = productService.liveCard(productId);
		items.save(new WishlistItem(userId, productId, product.priceFrom()));
	}

	@Transactional
	public void remove(Long userId, Long productId) {
		items.findByUserIdAndProductId(userId, productId).ifPresent(items::delete);
	}

	/** For the hearts on product cards: which of them are filled. */
	public List<Long> ids(Long userId) {
		return items.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(WishlistItem::getProductId).toList();
	}

	public List<WishlistEntry> list(Long userId) {
		List<WishlistItem> mine = items.findByUserIdOrderByCreatedAtDescIdDesc(userId);
		Map<Long, WatchedProduct> products = productService.watched(mine.stream().map(WishlistItem::getProductId).toList());
		return mine.stream()
			.filter(i -> products.containsKey(i.getProductId()))
			.map(i -> {
				WatchedProduct p = products.get(i.getProductId());
				return new WishlistEntry(p.card(), p.live(), i.getPriceWhenAdded(),
						dropOf(i.getPriceWhenAdded(), p.card().priceFrom()), i.getCreatedAt());
			})
			.toList();
	}

	private static BigDecimal dropOf(BigDecimal then, BigDecimal now) {
		return now == null || now.compareTo(then) >= 0 ? BigDecimal.ZERO : then.subtract(now);
	}

}

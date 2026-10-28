package com.trivoko.wishlist;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.trivoko.catalog.PriceChanged;
import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.WatchedProduct;
import com.trivoko.notify.NotificationKind;
import com.trivoko.notify.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * Price-drop alerts that are never spammy (Phase 9 step 4).
 *
 * <pre>
 *   changePrice ──PriceChanged──> onPriceChanged: price_changed = TRUE on the rows of that product
 *                                    (5 changes in a minute = the same rows marked 5 times = still ONE mark)
 *   PriceAlertJob, every 10 min ──> runOnce: for each marked row read the price NOW;
 *        at least 1% below what the customer last heard from us?  -> bell + email, remember this price
 *        always: clear the mark
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class PriceAlertService {

	/** A drop smaller than this (in % of the reference price) is not worth a message. */
	static final BigDecimal MIN_DROP_PERCENT = BigDecimal.ONE;

	private static final Locale INDIA = Locale.of("en", "IN");

	private final WishlistRepository items;

	private final ProductService productService;

	private final NotificationService notifications;

	/** Runs only after the price change is COMMITTED (a rolled-back edit marks nothing), in its own transaction. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPriceChanged(PriceChanged event) {
		items.flagProduct(event.productId());
	}

	/** One pass of the job. Returns how many alerts were sent. Tests call this directly. */
	@Transactional
	public int runOnce() {
		List<WishlistItem> marked = items.findByPriceChangedTrue();
		if (marked.isEmpty()) {
			return 0;
		}
		Map<Long, WatchedProduct> products = productService.watched(
				marked.stream().map(WishlistItem::getProductId).distinct().toList());
		int sent = 0;
		for (WishlistItem item : marked) {
			item.setPriceChanged(false);
			WatchedProduct product = products.get(item.getProductId());
			if (product == null || !product.live() || !product.card().inStock() || product.card().priceFrom() == null) {
				continue; // a blocked shop or an empty shelf: nothing the customer can buy, so no message
			}
			BigDecimal now = product.card().priceFrom();
			BigDecimal before = item.referencePrice();
			if (isRealDrop(before, now)) {
				tell(item.getUserId(), product.card(), before, now);
				item.setLastAlertedPrice(now);
				sent++;
			}
		}
		return sent;
	}

	/** now <= before x (100 - 1)% : 12999 -> 12900 is only 0.76% (no), 12999 -> 12860 is 1.07% (yes). */
	static boolean isRealDrop(BigDecimal before, BigDecimal now) {
		BigDecimal limit = before.multiply(BigDecimal.valueOf(100).subtract(MIN_DROP_PERCENT))
			.divide(BigDecimal.valueOf(100));
		return now.compareTo(limit) <= 0;
	}

	private void tell(Long userId, ProductCard product, BigDecimal before, BigDecimal now) {
		notifications.create(userId, NotificationKind.PRICE_DROP, "Price drop: " + product.name(),
				product.name() + " is now " + rupees(now) + " (was " + rupees(before) + "). It is on your wishlist.",
				"/products/" + product.slug(), true);
	}

	/** Indian grouping: ₹11,800 and ₹1,24,999 (no ".00" for whole rupees). */
	static String rupees(BigDecimal amount) {
		NumberFormat format = NumberFormat.getNumberInstance(INDIA);
		format.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
		format.setMaximumFractionDigits(2);
		return "₹" + format.format(amount);
	}

}

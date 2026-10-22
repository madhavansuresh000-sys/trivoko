package com.trivoko.flashsale;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.CartVariant;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.flashsale.dto.FlashDtos.ItemView;
import com.trivoko.flashsale.dto.FlashDtos.OfferRequest;
import com.trivoko.flashsale.dto.FlashDtos.SaleRequest;
import com.trivoko.flashsale.dto.FlashDtos.SaleSummary;
import com.trivoko.flashsale.dto.FlashDtos.SaleView;
import com.trivoko.flashsale.dto.FlashDtos.SellerOfferView;

import lombok.RequiredArgsConstructor;

/**
 * Setting up a sale and showing it (spec: Phase 7).
 *
 * <pre>
 *   admin schedules a window -> sellers offer a variant (price + quantity)
 *      offering TAKES the quantity out of normal stock (so nobody can buy it the normal way)
 *   sale runs  -> FlashClaimService.claim (atomic)
 *   sale ends  -> settleEnded: unsold units go back to normal stock
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashSaleService {

	private static final Logger log = LoggerFactory.getLogger(FlashSaleService.class);

	/** Claims that were in flight at the very end finish before the leftovers are counted. */
	static final Duration SETTLE_GRACE = Duration.ofSeconds(30);

	private static final Duration LONGEST_SALE = Duration.ofHours(48);

	private final FlashSaleRepository sales;

	private final FlashItemRepository items;

	private final FlashClaimRepository claims;

	private final ProductService productService;

	// ---------- admin ----------

	@Transactional
	public SaleSummary create(SaleRequest request) {
		if (!request.endsAt().isAfter(request.startsAt())) {
			throw new BadRequestException("The sale must end after it starts.");
		}
		if (Duration.between(request.startsAt(), request.endsAt()).compareTo(LONGEST_SALE) > 0) {
			throw new BadRequestException("A flash sale can last 48 hours at most.");
		}
		if (!request.endsAt().isAfter(LocalDateTime.now())) {
			throw new BadRequestException("The sale must end in the future.");
		}
		FlashSale sale = sales.save(new FlashSale(request.title().trim(), request.startsAt(), request.endsAt()));
		return summary(sale, List.of());
	}

	public List<SaleSummary> all() {
		List<FlashSale> list = sales.findAllByOrderByStartsAtDesc();
		Map<Long, List<FlashSaleItem>> byOffer = groupBySale(list);
		return list.stream().map(s -> summary(s, byOffer.getOrDefault(s.getId(), List.of()))).toList();
	}

	// ---------- sellers ----------

	/** The seller offers a variant. Only before the sale starts; the units leave normal stock right now. */
	@Transactional
	public SellerOfferView offer(Long sellerId, Long saleId, OfferRequest request) {
		FlashSale sale = sale(saleId);
		LocalDateTime now = LocalDateTime.now();
		if (!sale.getStartsAt().isAfter(now)) {
			throw new BusinessRuleException("This sale has already started: deals can only be added before it starts.");
		}
		CartVariant v = productService.cartVariants(List.of(request.variantId())).get(request.variantId());
		if (v == null || !v.sellerId().equals(sellerId)) {
			throw new ResourceNotFoundException("Variant", request.variantId());
		}
		if (!v.available()) {
			throw new BusinessRuleException("Only a live product can be put in a sale.");
		}
		if (request.salePrice().compareTo(v.price()) >= 0) {
			throw new BusinessRuleException("The sale price must be lower than the normal price (₹" + v.price().toPlainString() + ").");
		}
		if (items.findBySaleIdOrderByIdAsc(saleId).stream().anyMatch(i -> i.getVariantId().equals(v.variantId()))) {
			throw new BusinessRuleException("This item is already in this sale.");
		}
		if (sales.countOverlapping(v.variantId(), saleId, sale.getStartsAt(), sale.getEndsAt()) > 0) {
			throw new BusinessRuleException("This item is already in another sale at the same time.");
		}
		// out of normal stock NOW (throws "only N left" if the shelf has less)
		productService.holdStock(Map.of(v.variantId(), request.quantity()));
		FlashSaleItem item = items.save(new FlashSaleItem(saleId, v.variantId(), sellerId, request.salePrice(), request.quantity()));
		log.info("Flash deal: sale {} variant {} x{} at {}", saleId, v.variantId(), request.quantity(), request.salePrice());
		return offerView(item, sale, v, 0, now);
	}

	/** Changed my mind, before the sale starts: the units go back to normal stock. */
	@Transactional
	public void withdraw(Long sellerId, Long itemId) {
		FlashSaleItem item = items.findById(itemId).filter(i -> i.getSellerId().equals(sellerId))
			.orElseThrow(() -> new ResourceNotFoundException("Deal", itemId));
		FlashSale sale = sale(item.getSaleId());
		if (!sale.getStartsAt().isAfter(LocalDateTime.now())) {
			throw new BusinessRuleException("The sale has started: a deal cannot be withdrawn now.");
		}
		productService.releaseStock(Map.of(item.getVariantId(), item.getQuantity()));
		items.delete(item);
	}

	public List<SellerOfferView> offersOf(Long sellerId) {
		List<FlashSaleItem> mine = items.findBySellerIdOrderByIdDesc(sellerId);
		Map<Long, CartVariant> catalogue = productService.cartVariants(mine.stream().map(FlashSaleItem::getVariantId).toList());
		LocalDateTime now = LocalDateTime.now();
		List<SellerOfferView> out = new ArrayList<>();
		for (FlashSaleItem i : mine) {
			out.add(offerView(i, sale(i.getSaleId()), catalogue.get(i.getVariantId()), i.getSold(), now));
		}
		return out;
	}

	// ---------- the public pages ----------

	/** The sale to show on the home banner: the one running now, else the next one to start; none -> empty. */
	public java.util.Optional<SaleView> current() {
		LocalDateTime now = LocalDateTime.now();
		List<FlashSale> open = sales.findByEndsAtAfterOrderByStartsAtAsc(now);
		return open.stream().filter(s -> s.isLive(now)).findFirst()
			.or(() -> open.stream().findFirst())
			.map(s -> view(s, now));
	}

	public SaleView get(Long saleId) {
		return view(sale(saleId), LocalDateTime.now());
	}

	// ---------- the end of a sale ----------

	/** The job: sales that ended (plus a grace time) hand their unsold units back to normal stock, once. */
	@Transactional
	public int settleEnded() {
		int done = 0;
		for (FlashSale ended : sales.findBySettledFalseAndEndsAtBefore(LocalDateTime.now().minus(SETTLE_GRACE))) {
			FlashSale sale = sales.lockById(ended.getId()).orElseThrow(); // give-backs wait here
			if (sale.isSettled()) {
				continue;
			}
			Map<Long, Integer> back = new LinkedHashMap<>();
			for (FlashSaleItem i : items.findBySaleIdOrderByIdAsc(sale.getId())) {
				int unsold = i.getQuantity() - items.soldNow(i.getId());
				if (unsold > 0) {
					back.merge(i.getVariantId(), unsold, Integer::sum);
				}
			}
			productService.releaseStock(back);
			sales.markSettled(sale.getId());
			log.info("Flash sale {} settled: {} variants got unsold units back", sale.getId(), back.size());
			done++;
		}
		return done;
	}

	// ---------- helpers ----------

	FlashSale sale(Long id) {
		return sales.findById(id).orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));
	}

	private SaleView view(FlashSale sale, LocalDateTime now) {
		List<FlashSaleItem> deals = items.findBySaleIdOrderByIdAsc(sale.getId());
		Map<Long, CartVariant> catalogue = productService.cartVariants(deals.stream().map(FlashSaleItem::getVariantId).toList());
		List<ItemView> views = new ArrayList<>();
		for (FlashSaleItem i : deals) {
			CartVariant v = catalogue.get(i.getVariantId());
			if (v == null) {
				continue;
			}
			int left = i.left();
			views.add(new ItemView(i.getId(), v.variantId(), v.productSlug(), v.productName(), v.variantLabel(),
					v.imageUrl(), v.sellerName(), i.getSalePrice(), v.mrp(), i.getQuantity(), left, left == 0));
		}
		return new SaleView(sale.getId(), sale.getTitle(), sale.getStartsAt(), sale.getEndsAt(), now, state(sale, now), views);
	}

	private SellerOfferView offerView(FlashSaleItem i, FlashSale sale, CartVariant v, int claimed, LocalDateTime now) {
		return new SellerOfferView(i.getId(), sale.getId(), sale.getTitle(), sale.getStartsAt(), sale.getEndsAt(),
				v == null ? "?" : v.productName(), v == null ? "" : v.variantLabel(), i.getSalePrice(), i.getQuantity(),
				claimed, sale.getStartsAt().isAfter(now));
	}

	private SaleSummary summary(FlashSale s, List<FlashSaleItem> deals) {
		return new SaleSummary(s.getId(), s.getTitle(), s.getStartsAt(), s.getEndsAt(), state(s, LocalDateTime.now()),
				s.isSettled(), deals.size(), deals.stream().mapToInt(FlashSaleItem::getQuantity).sum(),
				deals.stream().mapToInt(FlashSaleItem::getSold).sum());
	}

	private Map<Long, List<FlashSaleItem>> groupBySale(List<FlashSale> list) {
		Map<Long, List<FlashSaleItem>> out = new LinkedHashMap<>();
		if (list.isEmpty()) {
			return out;
		}
		items.findBySaleIdIn(list.stream().map(FlashSale::getId).toList())
			.forEach(i -> out.computeIfAbsent(i.getSaleId(), k -> new ArrayList<>()).add(i));
		return out;
	}

	static String state(FlashSale s, LocalDateTime now) {
		if (now.isBefore(s.getStartsAt())) {
			return "UPCOMING";
		}
		return now.isBefore(s.getEndsAt()) ? "LIVE" : "ENDED";
	}

}

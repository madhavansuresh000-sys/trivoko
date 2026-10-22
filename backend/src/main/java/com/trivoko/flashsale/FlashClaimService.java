package com.trivoko.flashsale;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.CartVariant;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.flashsale.dto.FlashDtos.ClaimView;

/**
 * The hot path of a flash sale: claiming a unit, giving it back, and the link to checkout.
 *
 * <pre>
 *   claim      : ONE atomic UPDATE (sold = sold + 1 WHERE sold < quantity AND the sale is live)
 *                + one claim row (UNIQUE item+user = one per person)         -> HELD for 10 minutes
 *   checkout   : a HELD claim becomes a line at the sale price (CheckoutService)  -> ORDERED
 *   paid       : PAID.   Not paid / not ordered in time: GIVE BACK (sold = sold - 1, claim row removed)
 * </pre>
 */
@Service
@Transactional(readOnly = true)
public class FlashClaimService {

	private static final Logger log = LoggerFactory.getLogger(FlashClaimService.class);

	private final FlashItemRepository items;

	private final FlashClaimRepository claims;

	private final FlashSaleRepository sales;

	private final ProductService productService;

	private final Duration holdTime;

	FlashClaimService(FlashItemRepository items, FlashClaimRepository claims, FlashSaleRepository sales,
			ProductService productService, @Value("${app.flash.hold-time}") Duration holdTime) {
		this.items = items;
		this.claims = claims;
		this.sales = sales;
		this.productService = productService;
		this.holdTime = holdTime;
	}

	/** A claim that checkout can turn into an order line. */
	public record CheckoutClaim(Long claimId, Long itemId, Long variantId, BigDecimal salePrice, ClaimStatus status,
			LocalDateTime expiresAt) {
	}

	/**
	 * Claim one unit of a deal. Throws a clear 409 when sold out / not live / already claimed. The caller must not
	 * be inside a bigger transaction: a duplicate claim fails on the UNIQUE key and undoes the +1 with it.
	 */
	@Transactional
	public ClaimView claim(Long userId, Long itemId) {
		LocalDateTime now = LocalDateTime.now();
		if (claims.existsByItemIdAndUserId(itemId, userId)) { // friendly message; the UNIQUE key is the real wall
			throw new BusinessRuleException("You already claimed this deal.");
		}
		// FAST "no": once a deal is sold out thousands still press the button. A plain read (no row lock) answers them
		// without queueing on the locked row. It can only be briefly out of date right after a give-back (try again).
		Integer left = items.leftNow(itemId);
		if (left != null && left <= 0) {
			throw new BusinessRuleException("Sold out!");
		}
		if (items.claim(itemId, now) == 0) {
			throw whyNot(itemId, now);
		}
		FlashClaim claim = claims.saveAndFlush(new FlashClaim(itemId, userId, now.plus(holdTime)));
		// a light answer on purpose (no product lookups on the hot path): the page then opens checkout / "your deals"
		return new ClaimView(claim.getId(), itemId, claim.getStatus().name(), claim.getExpiresAt(), now, null, null, null, null);
	}

	private BusinessRuleException whyNot(Long itemId, LocalDateTime now) {
		FlashSaleItem item = items.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Deal", itemId));
		FlashSale sale = sales.findById(item.getSaleId()).orElseThrow();
		if (now.isBefore(sale.getStartsAt())) {
			return new BusinessRuleException("This sale has not started yet.");
		}
		if (!now.isBefore(sale.getEndsAt())) {
			return new BusinessRuleException("This sale has ended.");
		}
		return new BusinessRuleException("Sold out!");
	}

	/** My held and ordered claims (the "your deals" box and the checkout). */
	public List<ClaimView> mine(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		List<FlashClaim> list = claims.findByUserIdAndStatusIn(userId, EnumSet.allOf(ClaimStatus.class));
		return list.stream().filter(c -> c.getStatus() != ClaimStatus.HELD || c.getExpiresAt().isAfter(now))
			.map(c -> view(c, now)).toList();
	}

	// ---------- the give-back ----------

	/**
	 * GIVE-BACK: an unpaid claim returns its unit. The sale row is locked first, so this never runs at the same
	 * moment as the end-of-sale count; if the sale was already settled, the unit goes straight to normal stock.
	 */
	@Transactional
	public void giveBack(FlashClaim claim) {
		FlashSaleItem item = items.findById(claim.getItemId()).orElseThrow();
		FlashSale sale = sales.lockById(item.getSaleId()).orElseThrow();
		claims.delete(claim);
		claims.flush();
		items.giveBack(item.getId());
		if (sale.isSettled()) {
			productService.releaseStock(Map.of(item.getVariantId(), 1));
		}
		log.info("Flash claim {} given back (deal {})", claim.getId(), item.getId());
	}

	/** The job: HELD claims whose 10 minutes are over (nobody went to checkout). Returns how many. */
	@Transactional
	public int expireHeld() {
		List<FlashClaim> old = claims.findByStatusAndExpiresAtBefore(ClaimStatus.HELD, LocalDateTime.now());
		old.forEach(this::giveBack);
		return old.size();
	}

	// ---------- the link to checkout (called by the order module) ----------

	/** Claims that checkout turns into lines: HELD and not expired, or already ORDERED in my unpaid order. */
	public List<CheckoutClaim> forCheckout(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		List<FlashClaim> mine = claims.findByUserIdAndStatusIn(userId, Set.of(ClaimStatus.HELD, ClaimStatus.ORDERED));
		List<CheckoutClaim> out = new ArrayList<>();
		for (FlashClaim c : mine) {
			if (c.getStatus() == ClaimStatus.HELD && !c.getExpiresAt().isAfter(now)) {
				continue; // expired: the job gives it back
			}
			items.findById(c.getItemId()).ifPresent(i -> out.add(new CheckoutClaim(c.getId(), i.getId(), i.getVariantId(),
					i.getSalePrice(), c.getStatus(), c.getExpiresAt())));
		}
		return out;
	}

	/** The order was placed: these claims now belong to it. */
	@Transactional
	public void attachToOrder(List<Long> claimIds, Long orderId) {
		claims.findAllById(claimIds).forEach(c -> {
			c.setStatus(ClaimStatus.ORDERED);
			c.setOrderId(orderId);
		});
	}

	/** The order was paid: the units are bought. */
	@Transactional
	public void orderPaid(Long orderId) {
		claims.findByOrderId(orderId).forEach(c -> c.setStatus(ClaimStatus.PAID));
	}

	/**
	 * The unpaid order ended (expired, or replaced by a changed checkout). A claim still inside its own 10 minutes
	 * goes back to HELD so the customer can check out again; an old one is given back.
	 */
	@Transactional
	public void orderEnded(Long orderId) {
		LocalDateTime now = LocalDateTime.now();
		for (FlashClaim c : claims.findByOrderId(orderId)) {
			if (c.getStatus() != ClaimStatus.ORDERED) {
				continue;
			}
			if (c.getExpiresAt().isAfter(now)) {
				c.setStatus(ClaimStatus.HELD);
				c.setOrderId(null);
			}
			else {
				giveBack(c);
			}
		}
	}

	private ClaimView view(FlashClaim c, LocalDateTime now) {
		FlashSaleItem item = items.findById(c.getItemId()).orElseThrow();
		CartVariant v = productService.cartVariants(List.of(item.getVariantId())).get(item.getVariantId());
		return new ClaimView(c.getId(), c.getItemId(), c.getStatus().name(), c.getExpiresAt(), now, item.getSalePrice(),
				v == null ? "?" : v.productName(), v == null ? "" : v.variantLabel(), v == null ? null : v.imageUrl());
	}

}

package com.trivoko.seller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.auth.AuthUser;

/**
 * The guard on every seller URL, used in @PreAuthorize, e.g.
 *
 * <pre>
 *   &#64;PreAuthorize("@sellerAccess.isActiveSeller()")
 * </pre>
 *
 * Like a shopkeeper's key card: it opens only YOUR storeroom, and only while your shop is open
 * (APPROVED). The admin blocking the shop switches the card off at once. (Same idea as EventHub's
 * ClubAccess.)
 */
@Component("sellerAccess")
@Transactional(readOnly = true)
public class SellerAccess {

	private final SellerRepository sellers;

	SellerAccess(SellerRepository sellers) {
		this.sellers = sellers;
	}

	/** Logged in AND owns a shop that is APPROVED right now. */
	public boolean isActiveSeller() {
		return activeShop() != null;
	}

	/** The id of the logged-in user's APPROVED shop; 403 if there is none. */
	public Long currentSellerId() {
		Seller shop = activeShop();
		if (shop == null) {
			throw new AccessDeniedException("You need an approved shop to do this.");
		}
		return shop.getId();
	}

	private Seller activeShop() {
		return AuthUser.current()
			.flatMap(user -> sellers.findByOwnerId(user.id()))
			.filter(shop -> shop.getStatus() == SellerStatus.APPROVED)
			.orElse(null);
	}

}

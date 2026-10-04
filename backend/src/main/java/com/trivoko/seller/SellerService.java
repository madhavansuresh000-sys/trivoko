package com.trivoko.seller;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.admin.AuditService;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.common.Slugs;
import com.trivoko.seller.dto.SellerApplicationView;
import com.trivoko.seller.dto.SellerApplyRequest;
import com.trivoko.seller.dto.SellerProfile;
import com.trivoko.seller.dto.SellerSummary;
import com.trivoko.user.Role;
import com.trivoko.user.UserService;

/**
 * The seller module's rules. Other modules call this class, never SellerRepository.
 *
 * <pre>
 *   customer --apply--> PENDING --approve--> APPROVED (+ SELLER role) --block--> BLOCKED
 *                          └--reject(reason)--> REJECTED --apply again--> PENDING
 * </pre>
 */
@Service
@Transactional(readOnly = true)
public class SellerService {

	private final SellerRepository sellers;

	private final UserService userService;

	private final AuditService audit;

	SellerService(SellerRepository sellers, UserService userService, AuditService audit) {
		this.sellers = sellers;
		this.userService = userService;
		this.audit = audit;
	}

	/** A shop page is public only after the admin has approved the shop. */
	public SellerProfile getPublicProfile(String slug) {
		Seller s = sellers.findBySlugAndStatus(slug, SellerStatus.APPROVED)
			.orElseThrow(() -> new ResourceNotFoundException("Seller", slug));
		return new SellerProfile(s.getShopName(), s.getSlug(), s.getCity(), s.getDescription(),
				s.getCreatedAt().toLocalDate());
	}

	/** The shop this user owns (any status), or empty if they never applied. */
	public Optional<SellerSummary> findByOwner(Long userId) {
		return sellers.findByOwnerId(userId).map(SellerService::summary);
	}

	// ---------- "Become a seller" ----------

	/**
	 * One user = one shop. First time: a new PENDING shop. REJECTED before: the same shop goes back to
	 * PENDING with the new details. Already PENDING or APPROVED: 409. BLOCKED: 403, no second chance.
	 */
	@Transactional
	public SellerApplicationView apply(Long userId, SellerApplyRequest request) {
		Seller shop = sellers.findByOwnerId(userId).orElse(null);
		if (shop == null) {
			shop = new Seller(userId, request.shopName().trim(), uniqueSlug(request.shopName(), null),
					request.city().trim(), blankToNull(request.description()), SellerStatus.PENDING);
		}
		else {
			switch (shop.getStatus()) {
				case PENDING -> throw new BusinessRuleException("Your shop application is already waiting for approval.");
				case APPROVED -> throw new BusinessRuleException("You already have an approved shop.");
				case BLOCKED -> throw new AccessDeniedException("This shop is blocked and cannot apply again.");
				case REJECTED -> {
					if (!shop.getShopName().equals(request.shopName().trim())) {
						shop.setSlug(uniqueSlug(request.shopName(), shop.getId()));
					}
					shop.setShopName(request.shopName().trim());
					shop.setCity(request.city().trim());
					shop.setDescription(blankToNull(request.description()));
					shop.setStatus(SellerStatus.PENDING);
					shop.setRejectReason(null);
				}
			}
		}
		shop.setGstin(blankToNull(request.gstin()));
		return view(sellers.save(shop));
	}

	public SellerApplicationView myApplication(Long userId) {
		return sellers.findByOwnerId(userId).map(SellerService::view)
			.orElseThrow(() -> new ResourceNotFoundException("No shop application found for this account"));
	}

	// ---------- Admin decisions ----------

	public List<SellerApplicationView> listByStatus(SellerStatus status) {
		return sellers.findByStatusOrderByIdAsc(status).stream().map(SellerService::view).toList();
	}

	/** PENDING -> APPROVED, and the owner gets the SELLER role (it works at once, see JwtCookieFilter). */
	@Transactional
	public SellerApplicationView approve(Long sellerId) {
		Seller shop = pending(sellerId);
		shop.setStatus(SellerStatus.APPROVED);
		shop.setRejectReason(null);
		userService.addRole(shop.getOwnerId(), Role.SELLER);
		audit.record("SELLER_APPROVED", "SELLER", shop.getId(), shop.getShopName());
		return view(shop);
	}

	@Transactional
	public SellerApplicationView reject(Long sellerId, String reason) {
		Seller shop = pending(sellerId);
		shop.setStatus(SellerStatus.REJECTED);
		shop.setRejectReason(reason.trim());
		audit.record("SELLER_REJECTED", "SELLER", shop.getId(), reason.trim());
		return view(shop);
	}

	/**
	 * Any shop can be blocked. The SELLER role stays, but SellerAccess refuses every seller URL for a
	 * shop that is not APPROVED, so the block works at once.
	 */
	@Transactional
	public SellerApplicationView block(Long sellerId) {
		Seller shop = get(sellerId);
		if (shop.getStatus() == SellerStatus.BLOCKED) {
			throw new BusinessRuleException("This shop is already blocked.");
		}
		shop.setStatus(SellerStatus.BLOCKED);
		audit.record("SELLER_BLOCKED", "SELLER", shop.getId(), shop.getShopName());
		return view(shop);
	}

	/** For other modules (e.g. product approval): is this shop allowed to sell right now? */
	public boolean isApproved(Long sellerId) {
		return get(sellerId).getStatus() == SellerStatus.APPROVED;
	}

	// ---------- helpers ----------

	private Seller get(Long sellerId) {
		return sellers.findById(sellerId).orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));
	}

	private Seller pending(Long sellerId) {
		Seller shop = get(sellerId);
		if (shop.getStatus() != SellerStatus.PENDING) {
			throw new BusinessRuleException("Only a PENDING shop can be approved or rejected (this one is "
					+ shop.getStatus() + ").");
		}
		return shop;
	}

	/** "Ravi's Gadget Shop" -> "ravi-s-gadget-shop" (or "-2" ...). ownId = the shop being renamed (its own slug is free). */
	String uniqueSlug(String shopName, Long ownId) {
		return Slugs.unique(shopName, "shop", slug -> isTaken(slug, ownId));
	}

	private boolean isTaken(String slug, Long ownId) {
		return sellers.findBySlug(slug).filter(s -> !s.getId().equals(ownId)).isPresent();
	}

	static SellerSummary summary(Seller s) {
		return new SellerSummary(s.getId(), s.getShopName(), s.getSlug(), s.getStatus());
	}

	static SellerApplicationView view(Seller s) {
		return new SellerApplicationView(s.getId(), s.getOwnerId(), s.getShopName(), s.getSlug(), s.getCity(),
				s.getDescription(), s.getGstin(), s.getStatus(), s.getRejectReason());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}

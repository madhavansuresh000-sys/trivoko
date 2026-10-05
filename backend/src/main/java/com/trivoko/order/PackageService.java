package com.trivoko.order;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.order.dto.SellerPackageView;
import com.trivoko.payment.PaymentService;

import lombok.RequiredArgsConstructor;

/**
 * The seller's packing table and the cancel button (Phase 5). Every move goes through OrderPackage.moveTo, so
 * the state machine decides what is legal and every move lands on the customer's timeline.
 *
 * Two people changing the same package at the same moment (customer cancels, seller ships): the package row has
 * @Version, and saveAndFlush() makes the loser fail BEFORE any money moves -> 409 "someone else changed it".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageService {

	/** What a seller sees: from "placed" on (unpaid and expired packages are not theirs to handle). */
	private static final EnumSet<PackageStatus> VISIBLE = EnumSet.of(PackageStatus.PLACED, PackageStatus.PACKED,
			PackageStatus.SHIPPED, PackageStatus.DELIVERED, PackageStatus.CANCELLED);

	private final OrderPackageRepository packages;

	private final OrderService orders;

	private final ProductService productService;

	private final PaymentService paymentService;

	private final PackageNotifications notifications;

	public PageResponse<SellerPackageView> listForSeller(Long sellerId, PackageStatus status, Pageable pageable) {
		var statuses = status == null ? VISIBLE : EnumSet.of(status);
		return PageResponse.from(packages.findBySellerIdAndStatusInOrderByIdDesc(sellerId, statuses, pageable),
				SellerPackageView::of);
	}

	@Transactional
	public SellerPackageView pack(Long sellerId, Long packageId) {
		OrderPackage p = mine(sellerId, packageId);
		p.moveTo(PackageStatus.PACKED, PackageEvent.Actor.SELLER, "Packed by " + p.getSellerName());
		packages.saveAndFlush(p);
		notifications.moved(p);
		return SellerPackageView.of(p);
	}

	@Transactional
	public SellerPackageView ship(Long sellerId, Long packageId, String trackingNumber) {
		OrderPackage p = mine(sellerId, packageId);
		p.ship(trackingNumber.trim().toUpperCase(java.util.Locale.ROOT));
		packages.saveAndFlush(p);
		notifications.moved(p);
		return SellerPackageView.of(p);
	}

	@Transactional
	public SellerPackageView deliver(Long sellerId, Long packageId) {
		OrderPackage p = mine(sellerId, packageId);
		p.moveTo(PackageStatus.DELIVERED, PackageEvent.Actor.SELLER, "Delivered");
		packages.saveAndFlush(p);
		onDelivered(p);
		notifications.moved(p);
		return SellerPackageView.of(p);
	}

	@Transactional
	public SellerPackageView cancelBySeller(Long sellerId, Long packageId, String reason) {
		OrderPackage p = mine(sellerId, packageId);
		cancel(p, PackageEvent.Actor.SELLER, reason.trim());
		return SellerPackageView.of(p);
	}

	/** The customer's own order only (someone else's number = 404, like every order URL). */
	@Transactional
	public void cancelByCustomer(Long userId, String number, Long packageId) {
		Order order = orders.owned(userId, number);
		OrderPackage p = order.getPackages().stream().filter(x -> x.getId().equals(packageId)).findFirst()
			.orElseThrow(() -> new ResourceNotFoundException("Package", packageId));
		cancel(p, PackageEvent.Actor.CUSTOMER, "Cancelled by the customer");
	}

	/**
	 * Before shipping only (the state machine says so). Order: change + FLUSH first (the @Version check), only
	 * then stock back and money back - so a lost race never refunds anything.
	 */
	private void cancel(OrderPackage p, PackageEvent.Actor actor, String note) {
		p.moveTo(PackageStatus.CANCELLED, actor, note);
		packages.saveAndFlush(p);
		Map<Long, Integer> stock = new LinkedHashMap<>();
		p.getItems().forEach(i -> stock.merge(i.getVariantId(), i.getQuantity(), Integer::sum));
		productService.releaseStock(stock);
		paymentService.refundPart(p.getOrder().getId(), p.getTotal(), "refund-package-" + p.getId());
		notifications.moved(p);
	}

	/** Task 3 fills this: the seller's ledger line (+90%). */
	void onDelivered(OrderPackage p) {
	}

	/** Another shop's package -> 403 (it exists, but it is not yours). */
	private OrderPackage mine(Long sellerId, Long packageId) {
		OrderPackage p = packages.findById(packageId).orElseThrow(() -> new ResourceNotFoundException("Package", packageId));
		if (!p.getSellerId().equals(sellerId)) {
			throw new AccessDeniedException("This package belongs to another shop.");
		}
		return p;
	}

}

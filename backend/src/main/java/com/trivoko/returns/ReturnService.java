package com.trivoko.returns;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.admin.AuditService;
import com.trivoko.catalog.ImageUploadService;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.order.OrderService;
import com.trivoko.order.PackageStatus;
import com.trivoko.order.dto.OrderViews.ReturnableLine;
import com.trivoko.returns.dto.ReturnDtos.RequestReturn;
import com.trivoko.returns.dto.ReturnDtos.ReturnView;

import lombok.RequiredArgsConstructor;

/**
 * Returns & refunds (Phase 8). Every status change walks through ONE door: moveTo (steps 2-3). The buttons call it:
 * ask (step 4), approve / reject / auto-approve (5), escalate + admin (6), picked up -> refund (7-8).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

	/** How long after delivery a customer may still ask (spec section 3). */
	static final Duration RETURN_WINDOW = Duration.ofDays(7);

	/** A seller who does not answer in this time has said yes (spec section 2: the auto-approve job). */
	static final Duration SELLER_ANSWER_TIME = Duration.ofDays(2);

	/** How long after a "no" the customer may still ask the admin (spec section 2, default). */
	static final Duration ESCALATE_WINDOW = Duration.ofDays(3);

	private final ReturnRequestRepository returns;

	private final OrderService orders;

	private final ImageUploadService uploads;

	private final AuditService audit;

	/**
	 * The ONLY way a return changes status.
	 * <ol>
	 * <li>ask the state machine - an illegal move is a clear 409, nothing changes;</li>
	 * <li>change the status and write one timeline line;</li>
	 * <li>saveAndFlush NOW: if someone else changed this return a moment ago, @Version makes THIS call fail here -
	 * before any refund call or stock change that the caller would do next.</li>
	 * </ol>
	 */
	@Transactional
	public ReturnRequest moveTo(ReturnRequest ret, ReturnStatus next, ReturnEvent.Actor actor, String note) {
		ReturnStatus now = ret.getStatus();
		if (!now.canMoveTo(next)) {
			throw new BusinessRuleException("A " + now + " return cannot move to " + next + ".");
		}
		ret.setStatus(next);
		ret.addEvent(next, actor, note);
		return returns.saveAndFlush(ret);
	}

	// ---------- step 4: the customer asks ----------

	/**
	 * "Return this item". Rules: my own line (else 404), its package DELIVERED, at most 7 days ago, once per line.
	 * The refund amount is fixed NOW from the bill (line total minus its coupon share), so later price changes or
	 * coupon edits cannot change it. A double click at the same instant is stopped by UNIQUE(order_item_id).
	 */
	@Transactional
	public ReturnView request(Long customerId, RequestReturn req) {
		ReturnableLine line = orders.returnableLine(customerId, req.orderItemId());
		if (line.packageStatus() != PackageStatus.DELIVERED || line.deliveredAt() == null) {
			throw new BusinessRuleException("Only delivered items can be returned.");
		}
		if (LocalDateTime.now().isAfter(line.deliveredAt().plus(RETURN_WINDOW))) {
			throw new BusinessRuleException("The 7-day return window for this item has closed.");
		}
		if (returns.existsByOrderItemId(line.orderItemId())) {
			throw new BusinessRuleException("This item already has a return.");
		}
		String photo = blankToNull(req.photoUrl());
		if (photo != null && !uploads.isOurPhoto(photo)) {
			throw new BadRequestException("The photo must be uploaded through TriVoKo.");
		}
		ReturnRequest ret = new ReturnRequest(line.orderItemId(), line.packageId(), line.orderId(), customerId,
				line.sellerId(), req.reason(), req.note().trim(), photo, line.refundAmount());
		ret.addEvent(ReturnStatus.REQUESTED, ReturnEvent.Actor.CUSTOMER, req.note().trim());
		return ReturnView.of(returns.saveAndFlush(ret));
	}

	/** The customer changes their mind - only while the seller has not answered yet (REQUESTED). */
	@Transactional
	public ReturnView cancel(Long customerId, Long returnId) {
		ReturnRequest ret = mine(customerId, returnId);
		return ReturnView.of(moveTo(ret, ReturnStatus.CANCELLED, ReturnEvent.Actor.CUSTOMER, "Cancelled by the customer"));
	}

	// ---------- step 5: the seller answers ----------

	/** The seller's returns queue, newest first; status = null means all. */
	public PageResponse<ReturnView> forSeller(Long sellerId, ReturnStatus status, Pageable pageable) {
		return PageResponse.from(status == null ? returns.findBySellerId(sellerId, pageable)
				: returns.findBySellerIdAndStatus(sellerId, status, pageable), ReturnView::of);
	}

	@Transactional
	public ReturnView approve(Long sellerId, Long returnId) {
		return ReturnView.of(moveTo(ofSeller(sellerId, returnId), ReturnStatus.APPROVED, ReturnEvent.Actor.SELLER,
				"Approved by the seller - a courier will collect it"));
	}

	/** A "no" always carries a reason the customer sees (and the admin reads if they escalate). */
	@Transactional
	public ReturnView reject(Long sellerId, Long returnId, String reason) {
		ReturnRequest ret = ofSeller(sellerId, returnId);
		ret.setRejectReason(reason.trim());
		return ReturnView.of(moveTo(ret, ReturnStatus.REJECTED, ReturnEvent.Actor.SELLER, reason.trim()));
	}

	/**
	 * The job: REQUESTED for more than 2 days with no answer -> APPROVED by the system, so a silent seller can never
	 * block a customer's money. Returns how many were approved.
	 */
	@Transactional
	public int autoApproveOld() {
		List<ReturnRequest> waiting = returns.findByStatusAndCreatedAtBefore(ReturnStatus.REQUESTED,
				LocalDateTime.now().minus(SELLER_ANSWER_TIME));
		waiting.forEach(r -> moveTo(r, ReturnStatus.APPROVED, ReturnEvent.Actor.SYSTEM,
				"Approved automatically - the seller did not answer in 2 days"));
		return waiting.size();
	}

	/** Another shop's return is "not found" (404). */
	private ReturnRequest ofSeller(Long sellerId, Long returnId) {
		return returns.findByIdAndSellerId(returnId, sellerId)
			.orElseThrow(() -> new ResourceNotFoundException("Return", returnId));
	}

	// ---------- step 6: escalate once, the admin decides ----------

	/**
	 * The customer disagrees with the seller's "no" and asks the admin - once (ESCALATED never goes back to REJECTED),
	 * and within 3 days of the rejection. The rejection time is the return's last change (nothing else moves a
	 * REJECTED return).
	 */
	@Transactional
	public ReturnView escalate(Long customerId, Long returnId, String note) {
		ReturnRequest ret = mine(customerId, returnId);
		if (ret.getStatus() == ReturnStatus.REJECTED
				&& LocalDateTime.now().isAfter(ret.getUpdatedAt().plus(ESCALATE_WINDOW))) {
			throw new BusinessRuleException("You can ask the admin within 3 days of the seller's answer.");
		}
		ret.setEscalatedAt(LocalDateTime.now());
		return ReturnView.of(moveTo(ret, ReturnStatus.ESCALATED, ReturnEvent.Actor.CUSTOMER, note.trim()));
	}

	/** The admin's disputes page: one status (ESCALATED by default in the controller), oldest first is fairest. */
	public PageResponse<ReturnView> forAdmin(ReturnStatus status, Pageable pageable) {
		return PageResponse.from(returns.findByStatus(status, pageable), ReturnView::of);
	}

	/** The admin sides with the customer: the return goes on as if the seller had said yes. Final. */
	@Transactional
	public ReturnView adminApprove(Long returnId, String note) {
		ReturnRequest ret = any(returnId);
		String why = note == null || note.isBlank() ? "Approved by TriVoKo after review" : note.trim();
		moveTo(ret, ReturnStatus.APPROVED, ReturnEvent.Actor.ADMIN, why);
		audit.record("RETURN_APPROVED", "RETURN", ret.getId(), why);
		return ReturnView.of(ret);
	}

	/** The admin agrees with the seller: the return is closed for good, with a reason the customer sees. */
	@Transactional
	public ReturnView adminClose(Long returnId, String reason) {
		ReturnRequest ret = any(returnId);
		moveTo(ret, ReturnStatus.CLOSED, ReturnEvent.Actor.ADMIN, reason.trim());
		audit.record("RETURN_CLOSED", "RETURN", ret.getId(), reason.trim());
		return ReturnView.of(ret);
	}

	private ReturnRequest any(Long returnId) {
		return returns.findById(returnId).orElseThrow(() -> new ResourceNotFoundException("Return", returnId));
	}

	// ---------- the customer's lists ----------

	public List<ReturnView> mine(Long customerId) {
		return returns.findByCustomerIdOrderByIdDesc(customerId).stream().map(ReturnView::of).toList();
	}

	public ReturnView one(Long customerId, Long returnId) {
		return ReturnView.of(mine(customerId, returnId));
	}

	/** Someone else's return is simply "not found" (404). */
	private ReturnRequest mine(Long customerId, Long returnId) {
		return returns.findByIdAndCustomerId(returnId, customerId)
			.orElseThrow(() -> new ResourceNotFoundException("Return", returnId));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}

package com.trivoko.returns;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ImageUploadService;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.BusinessRuleException;
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

	private final ReturnRequestRepository returns;

	private final OrderService orders;

	private final ImageUploadService uploads;

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

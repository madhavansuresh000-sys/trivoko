package com.trivoko.returns;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.common.BusinessRuleException;

import lombok.RequiredArgsConstructor;

/**
 * Returns & refunds (Phase 8). Step 2 is the one door every status change walks through: moveTo. The buttons
 * (ask, approve, reject, escalate, picked up, refund) arrive in steps 4-8 and all call moveTo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

	private final ReturnRequestRepository returns;

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

}

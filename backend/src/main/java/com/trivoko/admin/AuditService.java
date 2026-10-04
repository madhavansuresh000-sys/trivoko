package com.trivoko.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.auth.AuthUser;

import lombok.RequiredArgsConstructor;

/**
 * Writes the audit log: who changed what and when. Other modules call record(...) after an important
 * change (approve, reject, block, price change). The "who" is taken from the logged-in user, so callers
 * cannot get it wrong.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

	private static final int MAX_DETAILS = 500;

	private final AuditLogRepository logs;

	/** Runs inside the caller's transaction: if the change is rolled back, so is its log line. */
	@Transactional
	public void record(String action, String entityType, Long entityId, String details) {
		Long actor = AuthUser.current().map(AuthUser::id).orElse(null);
		String shortDetails = details == null || details.length() <= MAX_DETAILS ? details : details.substring(0, MAX_DETAILS);
		logs.save(new AuditLog(actor, action, entityType, entityId, shortDetails));
	}

	/** Newest first. */
	@Transactional(readOnly = true)
	public List<AuditLog> latestFor(String entityType, Long entityId) {
		return logs.findByEntityTypeAndEntityIdOrderByIdDesc(entityType, entityId);
	}

}

package com.trivoko.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Only the admin module may use this (ArchUnit rule); other modules call AuditService. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByEntityTypeAndEntityIdOrderByIdDesc(String entityType, Long entityId);

	/** The activity log page, newest first (Phase 6). */
	Page<AuditLog> findAllByOrderByIdDesc(Pageable pageable);

	Page<AuditLog> findByActionOrderByIdDesc(String action, Pageable pageable);

}

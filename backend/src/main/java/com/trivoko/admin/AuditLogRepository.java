package com.trivoko.admin;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Only the admin module may use this (ArchUnit rule); other modules call AuditService. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByEntityTypeAndEntityIdOrderByIdDesc(String entityType, Long entityId);

}

package com.trivoko.admin;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line in the audit log, e.g. "user 1 did SELLER_APPROVED on SELLER 9 at 10:42". Never changed. */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** null = done by the system, not by a person. */
	@Column(name = "actor_user_id", updatable = false)
	private Long actorUserId;

	@Column(nullable = false, length = 40, updatable = false)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 30, updatable = false)
	private String entityType;

	@Column(name = "entity_id", nullable = false, updatable = false)
	private Long entityId;

	@Column(length = 500, updatable = false)
	private String details;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	AuditLog(Long actorUserId, String action, String entityType, Long entityId, String details) {
		this.actorUserId = actorUserId;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
	}

}

package com.trivoko.notify;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One message for one user: shown under the bell, and emailed when emailStatus is PENDING (from EventHub). */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The user module's id (a plain number: notify does not map user entities). */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationKind kind;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 1000)
	private String body;

	/** A page of the shop, e.g. "/orders/TV-100123" (the email turns it into a full link). */
	@Column(length = 300)
	private String link;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "email_status", nullable = false, length = 10)
	private EmailStatus emailStatus;

	@Column(name = "email_attempts", nullable = false)
	private int emailAttempts;

	@Column(name = "emailed_at")
	private LocalDateTime emailedAt;

}

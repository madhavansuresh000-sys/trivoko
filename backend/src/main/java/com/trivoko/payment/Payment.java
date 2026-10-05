package com.trivoko.payment;

import java.math.BigDecimal;
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

/** One payment page opened for an order (the Stripe "checkout session", or the fake test page). */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The order module's id (a plain number: the payment module does not map order entities). */
	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private PaymentProvider provider;

	@Column(name = "session_id", nullable = false, unique = true)
	private String sessionId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private PaymentStatus status = PaymentStatus.CREATED;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Setter
	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	public Payment(Long orderId, PaymentProvider provider, String sessionId, BigDecimal amount) {
		this.orderId = orderId;
		this.provider = provider;
		this.sessionId = sessionId;
		this.amount = amount;
	}

}

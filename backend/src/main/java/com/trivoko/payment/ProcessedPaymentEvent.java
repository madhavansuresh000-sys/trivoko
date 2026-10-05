package com.trivoko.payment;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "We already handled this message." Stripe may send the same webhook twice; the second time its id is
 * found here and nothing changes (idempotency - copied from EventHub).
 */
@Entity
@Table(name = "processed_payment_events")
@Getter
@NoArgsConstructor
public class ProcessedPaymentEvent {

	@Id
	@Column(name = "event_id")
	private String eventId;

	@Column(name = "processed_at", nullable = false)
	private LocalDateTime processedAt = LocalDateTime.now();

	public ProcessedPaymentEvent(String eventId) {
		this.eventId = eventId;
	}

}

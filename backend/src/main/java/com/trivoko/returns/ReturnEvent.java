package com.trivoko.returns;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line of the return timeline: "Approved by Kovai Sports - 23 Oct, 11:05 am". */
@Entity
@Table(name = "return_events")
@Getter
@NoArgsConstructor
public class ReturnEvent {

	/** Who made the move. */
	public enum Actor {
		CUSTOMER, SELLER, ADMIN, SYSTEM
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "return_id")
	private ReturnRequest returnRequest;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReturnStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Actor actor;

	@Column(length = 300)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	ReturnEvent(ReturnRequest returnRequest, ReturnStatus status, Actor actor, String note) {
		this.returnRequest = returnRequest;
		this.status = status;
		this.actor = actor;
		this.note = note;
	}

}

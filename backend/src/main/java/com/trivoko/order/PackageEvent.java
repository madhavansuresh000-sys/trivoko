package com.trivoko.order;

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

/** One line of the tracking timeline: "Shipped by Kovai Sports - AWB12345 - 5 Oct, 3:10 pm". */
@Entity
@Table(name = "package_events")
@Getter
@NoArgsConstructor
public class PackageEvent {

	/** Who made the move. */
	public enum Actor {
		SYSTEM, SELLER, CUSTOMER
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "package_id")
	private OrderPackage orderPackage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PackageStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Actor actor;

	@Column(length = 200)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	PackageEvent(OrderPackage orderPackage, PackageStatus status, Actor actor, String note) {
		this.orderPackage = orderPackage;
		this.status = status;
		this.actor = actor;
		this.note = note;
	}

}

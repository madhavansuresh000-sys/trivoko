package com.trivoko.seller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One line of a seller's account book (table seller_ledger). The balance is the SUM of the lines - never one
 * stored number - so every rupee can be explained ("+808.38 for TV-100123, package 2").
 */
@Entity
@Table(name = "seller_ledger")
@Getter
@NoArgsConstructor
public class LedgerEntry {

	/** SALE = +90% of a delivered package. REFUND = minus 90% of a returned line's refund (Phase 8). */
	public enum Kind {
		SALE, REFUND
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	/** The order module's package id (a plain number). */
	@Column(name = "package_id", nullable = false)
	private Long packageId;

	@Column(name = "order_number", nullable = false, length = 12)
	private String orderNumber;

	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Kind kind;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(length = 200)
	private String note;

	/** Phase 8: the return a REFUND line belongs to (NULL for SALE). Part of the "once" key - see V10. */
	@Column(name = "return_id")
	private Long returnId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	/** Phase 8: a REFUND line names its return (one line per return - the V10 key). */
	LedgerEntry(Long sellerId, Long packageId, String orderNumber, Long returnId, BigDecimal amount, String note) {
		this(sellerId, packageId, orderNumber, Kind.REFUND, amount, note);
		this.returnId = returnId;
	}

	LedgerEntry(Long sellerId, Long packageId, String orderNumber, Kind kind, BigDecimal amount, String note) {
		this.sellerId = sellerId;
		this.packageId = packageId;
		this.orderNumber = orderNumber;
		this.kind = kind;
		this.amount = amount;
		this.note = note;
	}

}

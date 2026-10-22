package com.trivoko.returns;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One customer's return of ONE order line (table return_requests). Ids of other modules' rows are plain numbers:
 * this module never touches the order, seller or user tables itself (ArchUnit rule).
 */
@Entity
@Table(name = "return_requests")
@Getter
@NoArgsConstructor
public class ReturnRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_item_id", nullable = false)
	private Long orderItemId;

	@Column(name = "package_id", nullable = false)
	private Long packageId;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "customer_id", nullable = false)
	private Long customerId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	/** Changed ONLY by ReturnService.moveTo (which asks ReturnStatus.allowedNext first). */
	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReturnStatus status = ReturnStatus.REQUESTED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReturnReason reason;

	@Column(nullable = false, length = 300)
	private String note;

	@Column(name = "photo_url", length = 500)
	private String photoUrl;

	/** Fixed when the customer asks: line total minus the line's coupon share. */
	@Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal refundAmount;

	@Setter
	@Column(name = "reject_reason", length = 300)
	private String rejectReason;

	@Setter
	@Column(name = "escalated_at")
	private LocalDateTime escalatedAt;

	/** Two clicks at the same moment (e.g. seller "picked up" twice): one wins, the other gets 409. */
	@Version
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	@OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<ReturnEvent> events = new ArrayList<>();

	public ReturnRequest(Long orderItemId, Long packageId, Long orderId, Long customerId, Long sellerId,
			ReturnReason reason, String note, String photoUrl, BigDecimal refundAmount) {
		this.orderItemId = orderItemId;
		this.packageId = packageId;
		this.orderId = orderId;
		this.customerId = customerId;
		this.sellerId = sellerId;
		this.reason = reason;
		this.note = note;
		this.photoUrl = photoUrl;
		this.refundAmount = refundAmount;
	}

	/** Adds one line to the timeline (ReturnService.moveTo calls this right after changing the status). */
	public void addEvent(ReturnStatus newStatus, ReturnEvent.Actor actor, String eventNote) {
		events.add(new ReturnEvent(this, newStatus, actor, eventNote));
	}

	@PreUpdate
	void touch() {
		updatedAt = LocalDateTime.now();
	}

}

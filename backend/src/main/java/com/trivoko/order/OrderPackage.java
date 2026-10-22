package com.trivoko.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.trivoko.common.BusinessRuleException;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One seller's box of an order (table "packages"; called OrderPackage because java.lang.Package exists).
 * Chennai Mobiles sees and ships only its own package. total = itemsTotal - discount + shippingFee.
 */
@Entity
@Table(name = "packages")
@Getter
@NoArgsConstructor
public class OrderPackage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "seller_name", nullable = false, length = 120)
	private String sellerName;

	/** Changed ONLY through moveTo(...), which checks the state machine and writes the timeline. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PackageStatus status = PackageStatus.PENDING_PAYMENT;

	@Column(name = "tracking_number", length = 30)
	private String trackingNumber;

	@Column(name = "cancel_reason", length = 200)
	private String cancelReason;

	@Column(name = "placed_at")
	private LocalDateTime placedAt;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	@Column(name = "items_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal itemsTotal;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal discount;

	@Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
	private BigDecimal shippingFee;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	/** Phase 5: the customer cancelling while the seller ships - only one of them wins. */
	@Version
	@Column(nullable = false)
	private long version;

	@OneToMany(mappedBy = "orderPackage", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItem> items = new ArrayList<>();

	/** The tracking timeline, oldest first. */
	@OneToMany(mappedBy = "orderPackage", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<PackageEvent> events = new ArrayList<>();

	public OrderPackage(Long sellerId, String sellerName, BigDecimal itemsTotal, BigDecimal discount,
			BigDecimal shippingFee, BigDecimal total) {
		this.sellerId = sellerId;
		this.sellerName = sellerName;
		this.itemsTotal = itemsTotal;
		this.discount = discount;
		this.shippingFee = shippingFee;
		this.total = total;
	}

	public void addItem(OrderItem item) {
		item.setOrderPackage(this);
		items.add(item);
	}

	/**
	 * The ONE way a package changes status: the state machine must allow it (else 409), and every move becomes a
	 * line of the customer's timeline.
	 */
	public void moveTo(PackageStatus next, PackageEvent.Actor actor, String note) {
		if (!status.canMoveTo(next)) {
			throw new BusinessRuleException("A " + status + " package cannot become " + next + ".");
		}
		status = next;
		switch (next) {
			case PLACED -> placedAt = LocalDateTime.now();
			case DELIVERED -> deliveredAt = LocalDateTime.now();
			case CANCELLED -> cancelReason = note;
			default -> {
			}
		}
		events.add(new PackageEvent(this, next, actor, note));
	}

	/** SHIPPED needs the courier's tracking number (AWB). */
	public void ship(String trackingNumber) {
		moveTo(PackageStatus.SHIPPED, PackageEvent.Actor.SELLER, "Tracking number " + trackingNumber);
		this.trackingNumber = trackingNumber;
	}

	void setOrder(Order order) {
		this.order = order;
	}

}

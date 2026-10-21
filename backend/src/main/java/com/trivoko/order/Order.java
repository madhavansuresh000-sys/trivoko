package com.trivoko.order;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One checkout = one payment = one order, split into one package per seller.
 * The delivery address is copied (ship_*), so editing the address book later never moves a parcel.
 * Table "orders" ("order" is an SQL word).
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** What people see and type: TV-100123. */
	@Column(nullable = false, unique = true, length = 12)
	private String number;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Column(name = "items_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal itemsTotal;

	@Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountTotal;

	@Column(name = "shipping_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal shippingTotal;

	@Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal grandTotal;

	@Column(name = "coupon_code", length = 30)
	private String couponCode;

	@Column(name = "ship_name", nullable = false, length = 100)
	private String shipName;

	@Column(name = "ship_phone", nullable = false, length = 10)
	private String shipPhone;

	@Column(name = "ship_line1", nullable = false, length = 160)
	private String shipLine1;

	@Column(name = "ship_line2", length = 160)
	private String shipLine2;

	@Column(name = "ship_city", nullable = false, length = 80)
	private String shipCity;

	@Column(name = "ship_state", nullable = false, length = 80)
	private String shipState;

	@Column(name = "ship_pincode", nullable = false, length = 6)
	private String shipPincode;

	@Column(name = "hold_expires_at", nullable = false)
	private LocalDateTime holdExpiresAt;

	@Setter
	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	/** Webhook and expiry job touching the same order at the same moment: one wins, the other retries. */
	@Version
	@Column(nullable = false)
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderPackage> packages = new ArrayList<>();

	public Order(String number, Long userId, BigDecimal itemsTotal, BigDecimal discountTotal, BigDecimal shippingTotal,
			BigDecimal grandTotal, String couponCode, ShipTo shipTo, LocalDateTime holdExpiresAt) {
		this.number = number;
		this.userId = userId;
		this.itemsTotal = itemsTotal;
		this.discountTotal = discountTotal;
		this.shippingTotal = shippingTotal;
		this.grandTotal = grandTotal;
		this.couponCode = couponCode;
		this.shipName = shipTo.name();
		this.shipPhone = shipTo.phone();
		this.shipLine1 = shipTo.line1();
		this.shipLine2 = shipTo.line2();
		this.shipCity = shipTo.city();
		this.shipState = shipTo.state();
		this.shipPincode = shipTo.pincode();
		this.holdExpiresAt = holdExpiresAt;
	}

	public void addPackage(OrderPackage orderPackage) {
		orderPackage.setOrder(this);
		packages.add(orderPackage);
	}

	/** The copied delivery address. */
	public record ShipTo(String name, String phone, String line1, String line2, String city, String state,
			String pincode) {
	}

	public ShipTo shipTo() {
		return new ShipTo(shipName, shipPhone, shipLine1, shipLine2, shipCity, shipState, shipPincode);
	}

}

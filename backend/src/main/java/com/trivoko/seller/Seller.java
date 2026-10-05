package com.trivoko.seller;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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

/** A shop on TriVoKo, e.g. Chennai Mobiles. One seller sells many products. */
@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
public class Seller {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The user who owns this shop (one user = one shop). Kept as a plain id, not a link to the User
	 * entity, because users belong to another module (module rule: talk through services only).
	 */
	@Column(name = "user_id", nullable = false, unique = true, updatable = false)
	private Long ownerId;

	@Column(name = "shop_name", nullable = false, length = 120)
	private String shopName;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(nullable = false, length = 80)
	private String city;

	/** Indian GST number, optional (15 characters). */
	@Column(length = 15)
	private String gstin;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SellerStatus status = SellerStatus.PENDING;

	/** Why the admin said no (shown to the applicant). */
	@Column(name = "reject_reason", length = 300)
	private String rejectReason;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Seller(Long ownerId, String shopName, String slug, String city, String description, SellerStatus status) {
		this.ownerId = ownerId;
		this.shopName = shopName;
		this.slug = slug;
		this.city = city;
		this.description = description;
		this.status = status;
	}

}

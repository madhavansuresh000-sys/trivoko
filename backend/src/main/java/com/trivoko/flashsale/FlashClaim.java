package com.trivoko.flashsale;

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

/** One customer's unit of one deal. UNIQUE(item, user) in the database = one per person. */
@Entity
@Table(name = "flash_sale_claims")
@Getter
@NoArgsConstructor
public class FlashClaim {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ClaimStatus status;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Setter
	@Column(name = "order_id")
	private Long orderId;

	public FlashClaim(Long itemId, Long userId, LocalDateTime expiresAt) {
		this.itemId = itemId;
		this.userId = userId;
		this.expiresAt = expiresAt;
		this.status = ClaimStatus.HELD;
	}

}

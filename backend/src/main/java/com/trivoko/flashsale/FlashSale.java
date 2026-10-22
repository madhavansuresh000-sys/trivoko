package com.trivoko.flashsale;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One scheduled sale window the admin creates; sellers then offer variants in it. */
@Entity
@Table(name = "flash_sales")
@Getter
@NoArgsConstructor
public class FlashSale {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(name = "ends_at", nullable = false)
	private LocalDateTime endsAt;

	/** The unsold units already went back to normal stock (FlashSaleService.settleEnded). */
	@Column(nullable = false)
	private boolean settled;

	public FlashSale(String title, LocalDateTime startsAt, LocalDateTime endsAt) {
		this.title = title;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	public boolean isLive(LocalDateTime now) {
		return !now.isBefore(startsAt) && now.isBefore(endsAt);
	}

}

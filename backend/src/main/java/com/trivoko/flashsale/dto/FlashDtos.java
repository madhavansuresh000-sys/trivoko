package com.trivoko.flashsale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Everything the flash-sale API sends and receives. */
public final class FlashDtos {

	private FlashDtos() {
	}

	/** Admin: schedule a sale. */
	public record SaleRequest(
			@NotBlank @Size(max = 120) String title,
			@NotNull LocalDateTime startsAt,
			@NotNull LocalDateTime endsAt) {
	}

	/** Seller: offer one variant in a sale. */
	public record OfferRequest(
			@NotNull Long variantId,
			@NotNull @DecimalMin("1.00") BigDecimal salePrice,
			@NotNull @Min(1) @Max(10000) Integer quantity) {
	}

	/** One deal on the sale page. left = what can still be claimed right now. */
	public record ItemView(
			Long itemId,
			Long variantId,
			String productSlug,
			String productName,
			String variantLabel,
			String imageUrl,
			String shopName,
			BigDecimal salePrice,
			BigDecimal mrp,
			int quantity,
			int left,
			boolean soldOut) {
	}

	/** A sale with its deals. serverNow = the server's clock: the countdown counts against it, not the phone's. */
	public record SaleView(
			Long id,
			String title,
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			LocalDateTime serverNow,
			String state,
			List<ItemView> items) {
	}

	/** Admin list row. */
	public record SaleSummary(
			Long id,
			String title,
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			String state,
			boolean settled,
			int deals,
			int units,
			int claimed) {
	}

	/** A unit I hold (claimed, not yet paid) or bought. */
	public record ClaimView(
			Long claimId,
			Long itemId,
			String status,
			LocalDateTime expiresAt,
			LocalDateTime serverNow,
			BigDecimal salePrice,
			String productName,
			String variantLabel,
			String imageUrl) {
	}

	/** Seller: my deal with the live numbers. */
	public record SellerOfferView(
			Long itemId,
			Long saleId,
			String saleTitle,
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			String productName,
			String variantLabel,
			BigDecimal salePrice,
			int quantity,
			int claimed,
			boolean canWithdraw) {
	}

}

package com.trivoko.review.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.trivoko.common.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Everything the reviews API sends and receives. */
public final class ReviewDtos {

	private ReviewDtos() {
	}

	/** Customer: write (or rewrite) my review. */
	public record ReviewRequest(
			@NotNull(message = "Pick 1 to 5 stars") @Min(value = 1, message = "Pick 1 to 5 stars")
			@Max(value = 5, message = "Pick 1 to 5 stars") Integer rating,
			@Size(max = 80, message = "The title can be at most 80 characters") String title,
			@NotBlank(message = "Tell other buyers what you think")
			@Size(min = 10, max = 1000, message = "The review must be 10 to 1000 characters") String body) {
	}

	/** One review on the product page. author = "Ravi K." (first name + initial, never the full name). */
	public record ReviewView(Long id, int rating, String title, String body, String author, boolean verifiedBuyer,
			LocalDateTime createdAt, LocalDateTime updatedAt) {
	}

	/** "4.3 out of 5 - 12 reviews" and the 5..1 bars. average is null when there are no reviews yet. */
	public record RatingSummary(BigDecimal average, long count, Map<Integer, Long> stars) {
	}

	public record ReviewPage(RatingSummary summary, PageResponse<ReviewView> reviews) {
	}

	/** The product page asks: may I review this, and what did I write before? */
	public record MyReview(boolean canReview, ReviewView review) {
	}

}

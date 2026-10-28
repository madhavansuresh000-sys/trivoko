package com.trivoko.review;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.review.dto.ReviewDtos.MyReview;
import com.trivoko.review.dto.ReviewDtos.ReviewPage;
import com.trivoko.review.dto.ReviewDtos.ReviewRequest;
import com.trivoko.review.dto.ReviewDtos.ReviewView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Reviews on the product page. Reading is public; ".../mine" needs a login (SecurityConfig). */
@RestController
@RequestMapping("/api/products/{slug}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Verified-buyer reviews and the rating summary")
public class ReviewController {

	private final ReviewService reviews;

	@GetMapping
	@Operation(summary = "Rating summary + reviews, newest first (10 per page)")
	public ReviewPage list(@PathVariable String slug, @RequestParam(defaultValue = "0") int page) {
		return reviews.forProduct(slug, page);
	}

	@GetMapping("/mine")
	@Operation(summary = "May I review this product, and my review if I wrote one")
	public MyReview mine(@AuthenticationPrincipal AuthUser user, @PathVariable String slug) {
		return reviews.mine(user.id(), slug);
	}

	@PutMapping("/mine")
	@Operation(summary = "Write or edit my review (only after the product was delivered to me)")
	public ReviewView write(@AuthenticationPrincipal AuthUser user, @PathVariable String slug,
			@Valid @RequestBody ReviewRequest request) {
		try {
			return reviews.write(user.id(), slug, request);
		}
		catch (DataIntegrityViolationException twice) {
			// two first-time saves at the same instant: UNIQUE(product_id, user_id) let only one through
			throw new BusinessRuleException("Your review was just saved - please reload the page.");
		}
	}

	@DeleteMapping("/mine")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete my review")
	public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable String slug) {
		reviews.delete(user.id(), slug);
	}

}

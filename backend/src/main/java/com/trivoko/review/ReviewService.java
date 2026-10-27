package com.trivoko.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.ProductRef;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.order.OrderService;
import com.trivoko.review.dto.ReviewDtos.MyReview;
import com.trivoko.review.dto.ReviewDtos.RatingSummary;
import com.trivoko.review.dto.ReviewDtos.ReviewPage;
import com.trivoko.review.dto.ReviewDtos.ReviewRequest;
import com.trivoko.review.dto.ReviewDtos.ReviewView;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Verified-buyer reviews (Phase 9 step 2). Only a customer whose package with this product was DELIVERED may
 * write one - the order module answers that, we never read its tables. After every write the product's
 * average and count are recalculated and stored on the product (so listing cards need no extra query).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

	public static final int PAGE_SIZE = 10;

	private final ReviewRepository reviews;

	private final ProductService productService;

	private final OrderService orderService;

	private final UserService userService;

	/** The product page: summary + one page of reviews, newest first. Public. */
	public ReviewPage forProduct(String slug, int page) {
		ProductRef product = productService.liveRef(slug);
		Page<Review> found = reviews.findByProductIdOrderByCreatedAtDescIdDesc(product.id(),
				PageRequest.of(Math.max(page, 0), PAGE_SIZE));
		Map<Long, String> names = userService.namesOf(found.map(Review::getUserId).toSet());
		return new ReviewPage(summaryOf(product.id()),
				PageResponse.from(found, r -> view(r, names.get(r.getUserId()))));
	}

	public MyReview mine(Long userId, String slug) {
		ProductRef product = productService.liveRef(slug);
		ReviewView review = reviews.findByProductIdAndUserId(product.id(), userId)
			.map(r -> view(r, nameOf(userId)))
			.orElse(null);
		return new MyReview(orderService.hasDelivered(userId, product.variantIds()), review);
	}

	/** Create the first time, edit after that: one review per customer per product. */
	@Transactional
	public ReviewView write(Long userId, String slug, ReviewRequest request) {
		ProductRef product = productService.liveRef(slug);
		if (!orderService.hasDelivered(userId, product.variantIds())) {
			throw new AccessDeniedException("Only customers who received this product can review it.");
		}
		Review review = reviews.findByProductIdAndUserId(product.id(), userId)
			.orElseGet(() -> new Review(product.id(), userId));
		review.write(request.rating(), blankToNull(request.title()), request.body().trim());
		reviews.saveAndFlush(review);
		refreshRating(product.id());
		return view(review, nameOf(userId));
	}

	@Transactional
	public void delete(Long userId, String slug) {
		ProductRef product = productService.liveRef(slug);
		Review review = reviews.findByProductIdAndUserId(product.id(), userId)
			.orElseThrow(() -> new ResourceNotFoundException("Review of", slug));
		reviews.delete(review);
		reviews.flush();
		refreshRating(product.id());
	}

	private void refreshRating(Long productId) {
		RatingSummary summary = summaryOf(productId);
		productService.updateRating(productId, summary.average(), (int) summary.count());
	}

	/** One GROUP BY query gives the 5..1 bars; the count and the average come from the same numbers. */
	private RatingSummary summaryOf(Long productId) {
		Map<Integer, Long> stars = new LinkedHashMap<>();
		for (int s = 5; s >= 1; s--) {
			stars.put(s, 0L);
		}
		for (Object[] row : reviews.countByRating(productId)) {
			stars.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
		}
		long count = stars.values().stream().mapToLong(Long::longValue).sum();
		long total = stars.entrySet().stream().mapToLong(e -> e.getKey() * e.getValue()).sum();
		BigDecimal average = count == 0 ? null
				: BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
		return new RatingSummary(average, count, stars);
	}

	private String nameOf(Long userId) {
		return userService.namesOf(List.of(userId)).get(userId);
	}

	private static ReviewView view(Review r, String fullName) {
		return new ReviewView(r.getId(), r.getRating(), r.getTitle(), r.getBody(), shortName(fullName), true,
				r.getCreatedAt(), r.getUpdatedAt());
	}

	/** "Ravi Kumar" -> "Ravi K." (privacy: other shoppers never see a full name). */
	static String shortName(String fullName) {
		if (fullName == null || fullName.isBlank()) {
			return "TriVoKo customer";
		}
		String[] parts = fullName.trim().split("\\s+");
		return parts.length == 1 ? parts[0]
				: parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}

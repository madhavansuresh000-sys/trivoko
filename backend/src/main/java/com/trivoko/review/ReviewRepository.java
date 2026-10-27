package com.trivoko.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

	/** Newest first, for the product page. */
	Page<Review> findByProductIdOrderByCreatedAtDescIdDesc(Long productId, Pageable pageable);

	/** One row per star value: [rating, count] - the 5..1 bars and the average both come from this. */
	@Query("select r.rating, count(r) from Review r where r.productId = :productId group by r.rating")
	List<Object[]> countByRating(Long productId);

}

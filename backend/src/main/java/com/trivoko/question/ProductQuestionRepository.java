package com.trivoko.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

	/** The product page: newest first. */
	Page<ProductQuestion> findByProductIdOrderByCreatedAtDescIdDesc(Long productId, Pageable pageable);

	/** The seller's list: still waiting for an answer, oldest first (first come, first answered). */
	Page<ProductQuestion> findBySellerIdAndAnswerIsNullOrderByCreatedAtAscIdAsc(Long sellerId, Pageable pageable);

	/** The seller's list: already answered, latest answer first. */
	Page<ProductQuestion> findBySellerIdAndAnswerIsNotNullOrderByAnsweredAtDescIdDesc(Long sellerId, Pageable pageable);

	/** Anti-spam: open questions of one shopper on one product. */
	long countByProductIdAndAskerIdAndAnswerIsNull(Long productId, Long askerId);

	long countBySellerIdAndAnswerIsNull(Long sellerId);

}

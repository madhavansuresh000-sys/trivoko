package com.trivoko.question;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "Does this case fit the V12 5G?" - "Yes, it is made for the V12 and V12 5G." One question, one answer from the
 * seller (who may edit it). Ids are plain numbers: this module never touches the catalog, seller or user tables.
 */
@Entity
@Table(name = "product_questions")
@Getter
@NoArgsConstructor
public class ProductQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(name = "seller_id", nullable = false, updatable = false)
	private Long sellerId;

	@Column(name = "asker_id", nullable = false, updatable = false)
	private Long askerId;

	@Column(nullable = false, length = 300, updatable = false)
	private String question;

	@Column(length = 1000)
	private String answer;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "answered_at")
	private LocalDateTime answeredAt;

	public ProductQuestion(Long productId, Long sellerId, Long askerId, String question) {
		this.productId = productId;
		this.sellerId = sellerId;
		this.askerId = askerId;
		this.question = question;
	}

	/** The first answer or a correction; answeredAt keeps the time of the latest answer. */
	public void answer(String answer) {
		this.answer = answer;
		this.answeredAt = LocalDateTime.now();
	}

	public boolean isAnswered() {
		return answer != null;
	}

}

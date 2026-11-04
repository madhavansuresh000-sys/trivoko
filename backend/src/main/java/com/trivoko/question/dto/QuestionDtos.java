package com.trivoko.question.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Everything the questions API sends and receives. */
public final class QuestionDtos {

	private QuestionDtos() {
	}

	public record AskRequest(
			@NotBlank(message = "Type your question")
			@Size(min = 10, max = 300, message = "The question must be 10 to 300 characters") String question) {
	}

	public record AnswerRequest(
			@NotBlank(message = "Type your answer")
			@Size(min = 2, max = 1000, message = "The answer must be 2 to 1000 characters") String answer) {
	}

	/** On the product page. askedBy = "Ravi K."; answer and answeredAt are null while the seller has not answered. */
	public record QuestionView(Long id, String question, String askedBy, LocalDateTime createdAt, String answer,
			LocalDateTime answeredAt) {
	}

	/** In the seller's list: the same plus which product it is about. */
	public record SellerQuestionView(Long id, String productName, String productSlug, String question, String askedBy,
			LocalDateTime createdAt, String answer, LocalDateTime answeredAt) {
	}

}

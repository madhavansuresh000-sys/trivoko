package com.trivoko.question;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.PageResponse;
import com.trivoko.question.dto.QuestionDtos.AnswerRequest;
import com.trivoko.question.dto.QuestionDtos.QuestionView;
import com.trivoko.question.dto.QuestionDtos.SellerQuestionView;
import com.trivoko.seller.SellerAccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The seller's side: questions about MY products and my answers. */
@RestController
@RequestMapping("/api/seller/questions")
@PreAuthorize("@sellerAccess.isActiveSeller()")
@RequiredArgsConstructor
@Tag(name = "Seller questions", description = "Answer shoppers' questions about your products")
public class SellerQuestionController {

	private final QuestionService questions;

	private final SellerAccess sellerAccess;

	@GetMapping
	@Operation(summary = "answered=false: waiting for my answer (oldest first); true: answered (latest first)")
	public PageResponse<SellerQuestionView> list(@RequestParam(defaultValue = "false") boolean answered,
			@RequestParam(defaultValue = "0") int page) {
		return questions.forSeller(sellerAccess.currentSellerId(), answered, page);
	}

	@PutMapping("/{id}/answer")
	@Operation(summary = "Answer (or correct my answer to) a question about one of my products")
	public QuestionView answer(@PathVariable Long id, @Valid @RequestBody AnswerRequest request) {
		return questions.answer(sellerAccess.currentSellerId(), id, request);
	}

}

package com.trivoko.question;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.auth.AuthUser;
import com.trivoko.common.PageResponse;
import com.trivoko.question.dto.QuestionDtos.AskRequest;
import com.trivoko.question.dto.QuestionDtos.QuestionView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Questions on the product page. Reading is public (GET /api/products/**); asking needs a login. */
@RestController
@RequestMapping("/api/products/{slug}/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Shoppers ask about a product, its seller answers")
public class QuestionController {

	private final QuestionService questions;

	@GetMapping
	@Operation(summary = "Questions and answers about this product, newest first (10 per page)")
	public PageResponse<QuestionView> list(@PathVariable String slug, @RequestParam(defaultValue = "0") int page) {
		return questions.forProduct(slug, page);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Ask the seller a question (at most 3 unanswered per product)")
	public QuestionView ask(@AuthenticationPrincipal AuthUser user, @PathVariable String slug,
			@Valid @RequestBody AskRequest request) {
		return questions.ask(user.id(), slug, request);
	}

}

package com.trivoko.question;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.ProductRef;
import com.trivoko.common.BusinessRuleException;
import com.trivoko.common.PageResponse;
import com.trivoko.common.ResourceNotFoundException;
import com.trivoko.notify.NotificationKind;
import com.trivoko.notify.NotificationService;
import com.trivoko.question.dto.QuestionDtos.AnswerRequest;
import com.trivoko.question.dto.QuestionDtos.AskRequest;
import com.trivoko.question.dto.QuestionDtos.QuestionView;
import com.trivoko.question.dto.QuestionDtos.SellerQuestionView;
import com.trivoko.review.ReviewService;
import com.trivoko.seller.SellerService;
import com.trivoko.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Product questions and answers (after launch). Like asking the shopkeeper across the counter: any logged-in
 * shopper may ask about a product on sale, only that product's shop may answer, and both sides get a bell.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

	public static final int PAGE_SIZE = 10;

	/** A shopper may have at most this many unanswered questions on one product (stops spam). */
	static final int MAX_OPEN_PER_PRODUCT = 3;

	private final ProductQuestionRepository questions;

	private final ProductService productService;

	private final SellerService sellerService;

	private final UserService userService;

	private final NotificationService notificationService;

	/** The product page: newest first, answered or not. Public. */
	public PageResponse<QuestionView> forProduct(String slug, int page) {
		ProductRef product = productService.liveRef(slug);
		Page<ProductQuestion> found = questions.findByProductIdOrderByCreatedAtDescIdDesc(product.id(), pageOf(page));
		Map<Long, String> names = userService.namesOf(found.map(ProductQuestion::getAskerId).toSet());
		return PageResponse.from(found, q -> view(q, names));
	}

	@Transactional
	public QuestionView ask(Long userId, String slug, AskRequest request) {
		ProductRef product = productService.liveRef(slug);
		if (questions.countByProductIdAndAskerIdAndAnswerIsNull(product.id(), userId) >= MAX_OPEN_PER_PRODUCT) {
			throw new BusinessRuleException("You already have " + MAX_OPEN_PER_PRODUCT
					+ " questions waiting for an answer on this product. Please wait for the seller.");
		}
		Long sellerId = productService.sellerIdOf(product.id())
			.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
		ProductQuestion question = questions.save(new ProductQuestion(product.id(), sellerId, userId,
				request.question().trim()));
		sellerService.notifyOwner(sellerId, NotificationKind.QUESTION_ASKED, "New question about " + product.name(),
				shorten(question.getQuestion()), "/seller/questions", false);
		return view(question, userService.namesOf(Set.of(userId)));
	}

	/** The seller's list: waiting (oldest first) or answered (latest first). */
	public PageResponse<SellerQuestionView> forSeller(Long sellerId, boolean answered, int page) {
		Page<ProductQuestion> found = answered
				? questions.findBySellerIdAndAnswerIsNotNullOrderByAnsweredAtDescIdDesc(sellerId, pageOf(page))
				: questions.findBySellerIdAndAnswerIsNullOrderByCreatedAtAscIdAsc(sellerId, pageOf(page));
		Map<Long, String> names = userService.namesOf(found.map(ProductQuestion::getAskerId).toSet());
		Map<Long, ProductCard> products = productService.cardsInOrder(
				found.map(ProductQuestion::getProductId).stream().distinct().toList())
			.stream().collect(Collectors.toMap(ProductCard::id, Function.identity()));
		return PageResponse.from(found, q -> {
			ProductCard product = products.get(q.getProductId());
			return new SellerQuestionView(q.getId(), product == null ? "(removed product)" : product.name(),
					product == null ? null : product.slug(), q.getQuestion(), shortName(names, q.getAskerId()),
					q.getCreatedAt(), q.getAnswer(), q.getAnsweredAt());
		});
	}

	/** Only the shop that sells the product may answer; answering again corrects the answer. */
	@Transactional
	public QuestionView answer(Long sellerId, Long questionId, AnswerRequest request) {
		ProductQuestion question = questions.findById(questionId)
			.orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
		if (!question.getSellerId().equals(sellerId)) {
			throw new AccessDeniedException("Only the shop that sells this product can answer.");
		}
		boolean first = !question.isAnswered();
		question.answer(request.answer().trim());
		if (first) {
			notificationService.create(question.getAskerId(), NotificationKind.QUESTION_ANSWERED,
					"Your question was answered", shorten(question.getAnswer()), productLink(question), true);
		}
		return view(question, userService.namesOf(Set.of(question.getAskerId())));
	}

	private String productLink(ProductQuestion question) {
		return productService.cardsInOrder(List.of(question.getProductId())).stream().findFirst()
			.map(p -> "/products/" + p.slug() + "#questions").orElse("/");
	}

	private static QuestionView view(ProductQuestion q, Map<Long, String> names) {
		return new QuestionView(q.getId(), q.getQuestion(), shortName(names, q.getAskerId()), q.getCreatedAt(),
				q.getAnswer(), q.getAnsweredAt());
	}

	/** "Ravi K." - the same privacy rule as reviews. */
	private static String shortName(Map<Long, String> names, Long userId) {
		return ReviewService.shortName(names.get(userId));
	}

	private static PageRequest pageOf(int page) {
		return PageRequest.of(Math.max(page, 0), PAGE_SIZE);
	}

	/** Bells show one line: long text is cut at 120 characters. */
	private static String shorten(String text) {
		return text.length() <= 120 ? text : text.substring(0, 117) + "...";
	}

}

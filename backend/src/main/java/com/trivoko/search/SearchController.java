package com.trivoko.search;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.common.BadRequestException;
import com.trivoko.search.dto.SearchDtos.SearchQuery;
import com.trivoko.search.dto.SearchDtos.SearchResult;
import com.trivoko.search.dto.SearchDtos.Suggestion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Smart search (public, GET only - SecurityConfig). */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Typo-tolerant search with filters and counts, and suggestions while typing")
public class SearchController {

	/** The same limit as the product listing. */
	static final int MAX_PAGE_SIZE = 48;

	private final SearchService search;

	@GetMapping
	@Operation(summary = "Search products: forgives typos, best match first, with brand / category / price counts")
	public SearchResult search(
			@Parameter(description = "The words", example = "iphnoe case") @RequestParam(required = false) String q,
			@Parameter(description = "Category slug; a top category includes its sub-categories")
			@RequestParam(required = false) String category,
			@RequestParam(name = "brand", required = false) List<String> brands,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(defaultValue = "false") boolean inStock,
			@Parameter(description = "relevance, price or newest") @RequestParam(defaultValue = "relevance") String sort,
			@Parameter(description = "asc or desc (for price)") @RequestParam(defaultValue = "asc") String dir,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "24") int size) {
		if (page < 0) {
			throw new BadRequestException("page must be 0 or more");
		}
		return search.search(new SearchQuery(q, category, brands, minPrice, maxPrice, inStock, sort, dir), page,
				Math.clamp(size, 1, MAX_PAGE_SIZE));
	}

	@GetMapping("/suggest")
	@Operation(summary = "Suggestions while typing (from 2 letters, at most 8): brands first, then products")
	public List<Suggestion> suggest(@Parameter(example = "vol") @RequestParam(required = false) String q) {
		return search.suggest(q);
	}

}

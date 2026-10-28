package com.trivoko.search.dto;

import java.math.BigDecimal;
import java.util.List;

import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.common.PageResponse;

/** Everything the search API sends and receives. */
public final class SearchDtos {

	private SearchDtos() {
	}

	/** What the customer asked: the words, the ticked filters and the sort (relevance / newest / price). */
	public record SearchQuery(String q, String category, List<String> brands, BigDecimal minPrice, BigDecimal maxPrice,
			boolean inStock, String sort, String dir) {
	}

	/** "Volta (6)" in the brand list. */
	public record ValueCount(String value, long count) {
	}

	/** "Mobile phones (8)": slug for the link, name to show. */
	public record CategoryCount(String slug, String name, long count) {
	}

	/** "₹500 - ₹1,999 (4)": min / max are what the page sends back as minPrice / maxPrice (null = open end). */
	public record PriceBand(String label, BigDecimal min, BigDecimal max, long count) {
	}

	/** Each list ignores ITS OWN filter, so ticking Volta still shows Nimbus (3) to tick next. */
	public record Facets(List<ValueCount> brands, List<CategoryCount> categories, List<PriceBand> prices) {
	}

	public record SearchResult(PageResponse<ProductCard> results, Facets facets) {
	}

	/** One line under the search box: a product (opens it) or a brand (searches it). */
	public record Suggestion(String type, String text, String slug) {
	}

}

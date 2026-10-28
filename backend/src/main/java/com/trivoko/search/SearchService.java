package com.trivoko.search;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.CategoryService;
import com.trivoko.catalog.Product;
import com.trivoko.catalog.ProductService;
import com.trivoko.catalog.dto.ProductCard;
import com.trivoko.catalog.dto.SearchRow;
import com.trivoko.common.BadRequestException;
import com.trivoko.common.PageResponse;
import com.trivoko.search.dto.SearchDtos.CategoryCount;
import com.trivoko.search.dto.SearchDtos.Facets;
import com.trivoko.search.dto.SearchDtos.PriceBand;
import com.trivoko.search.dto.SearchDtos.SearchQuery;
import com.trivoko.search.dto.SearchDtos.SearchResult;
import com.trivoko.search.dto.SearchDtos.Suggestion;
import com.trivoko.search.dto.SearchDtos.ValueCount;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Smart search (Phase 10). Two helpers, each doing what it is good at (decision D11):
 *
 * <pre>
 *   1. Lucene: which products match these words (forgiving typos), best first   -> up to 500 ids
 *   2. MySQL:  which of them are live, in stock, in this category / brand / price -> always fresh
 *   3. Java:   facet counts and one page, still in Lucene's order
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

	/** Lucene hands over at most this many matches; MySQL filters them. (TriVoKo has 120 products.) */
	static final int MAX_MATCHES = 500;

	private static final int MAX_WORDS = 8;

	/** Suggestions start at the 2nd letter: after 1 letter almost everything matches. */
	static final int MIN_SUGGEST_LETTERS = 2;

	static final int MAX_SUGGESTIONS = 8;

	private static final int MAX_BRAND_SUGGESTIONS = 3;

	/** Look at a few more than 8 products: some may belong to a blocked shop and drop out. */
	private static final int SUGGEST_LOOKUP = 30;

	/** The price filter's ready-made bands. Prices have 2 decimals, so 499.99 closes the first band. */
	private static final List<PriceBand> BANDS = List.of(
			new PriceBand("Under ₹500", null, new BigDecimal("499.99"), 0),
			new PriceBand("₹500 - ₹1,999", new BigDecimal("500"), new BigDecimal("1999.99"), 0),
			new PriceBand("₹2,000 - ₹9,999", new BigDecimal("2000"), new BigDecimal("9999.99"), 0),
			new PriceBand("₹10,000 and above", new BigDecimal("10000"), null, 0));

	private final EntityManager em;

	private final ProductService productService;

	private final CategoryService categoryService;

	public SearchResult search(SearchQuery query, int page, int size) {
		List<String> words = wordsOf(query.q());
		if (words.isEmpty()) {
			throw new BadRequestException("Type at least one word to search for");
		}
		List<Long> ranked = matchingIds(words);

		// the fresh truth from MySQL; a product that is not live simply has no row
		Map<Long, SearchRow> live = productService.searchRows(ranked).stream()
			.collect(Collectors.toMap(SearchRow::id, Function.identity()));
		List<SearchRow> rows = ranked.stream().filter(live::containsKey).map(live::get).toList();

		Predicate<SearchRow> category = byCategory(query.category());
		Predicate<SearchRow> brand = r -> query.brands() == null || query.brands().isEmpty() || query.brands().contains(r.brand());
		Predicate<SearchRow> price = r -> inRange(r.priceFrom(), query.minPrice(), query.maxPrice());
		Predicate<SearchRow> stock = r -> !query.inStock() || r.inStock();

		List<SearchRow> results = sorted(rows.stream().filter(category.and(brand).and(price).and(stock)).toList(),
				query.sort(), query.dir());

		Facets facets = new Facets(
				brandCounts(rows.stream().filter(category.and(price).and(stock)).toList()),
				categoryCounts(rows.stream().filter(brand.and(price).and(stock)).toList()),
				priceCounts(rows.stream().filter(category.and(brand).and(stock)).toList()));

		List<Long> pageIds = results.stream().skip((long) page * size).limit(size).map(SearchRow::id).toList();
		List<ProductCard> cards = productService.cardsInOrder(pageIds);
		return new SearchResult(PageResponse.from(new PageImpl<>(cards, PageRequest.of(page, size), results.size()),
				Function.identity()), facets);
	}

	/**
	 * Suggestions while typing (Phase 10 step 4), from the 2nd letter: every typed word must be the START of a word
	 * in the name or brand ("vol" -> Volta, "silicone ca" -> Silicone case ...). No typo forgiveness here - while
	 * you are still typing, the word is not finished yet. Brands first (at most 3), then products; 8 lines in all.
	 */
	public List<Suggestion> suggest(String typed) {
		List<String> words = wordsOf(typed);
		if (words.isEmpty() || String.join("", words).length() < MIN_SUGGEST_LETTERS) {
			return List.of();
		}
		List<Long> ids = Search.session(em).search(Product.class)
			.select(f -> f.id(Long.class))
			.where(f -> {
				BooleanPredicateClausesStep<?, ?> all = f.bool();
				words.forEach(word -> all.must(f.match().field("name_suggest").boost(2f).field("brand_suggest").matching(word)));
				return all;
			})
			.fetchHits(SUGGEST_LOOKUP);
		// only live products (and so only brands that still have one); a blocked shop's products never show
		Set<Long> live = productService.searchRows(ids).stream().map(SearchRow::id).collect(Collectors.toSet());
		List<ProductCard> cards = productService.cardsInOrder(ids.stream().filter(live::contains).toList());

		String first = words.getFirst();
		List<Suggestion> brands = cards.stream().map(ProductCard::brand).distinct()
			.filter(b -> b.toLowerCase(Locale.ROOT).startsWith(first) && words.size() == 1)
			.limit(MAX_BRAND_SUGGESTIONS)
			.map(b -> new Suggestion("brand", b, null))
			.toList();
		List<Suggestion> products = cards.stream()
			.limit(MAX_SUGGESTIONS - brands.size())
			.map(c -> new Suggestion("product", c.name(), c.slug()))
			.toList();
		return Stream.concat(brands.stream(), products.stream()).toList();
	}

	/** "  iPhnoe   CASE " -> [iphnoe, case] (at most 8 words, so nobody can send a 5,000-word "search"). */
	static List<String> wordsOf(String q) {
		if (q == null || q.isBlank()) {
			return List.of();
		}
		return Arrays.stream(q.trim().toLowerCase(Locale.ROOT).split("\\s+")).limit(MAX_WORDS).toList();
	}

	/** Lucene: every word must match somewhere (name, brand, category, description); best match first. */
	private List<Long> matchingIds(List<String> words) {
		return Search.session(em).search(Product.class)
			.select(f -> f.id(Long.class))
			.where(f -> {
				BooleanPredicateClausesStep<?, ?> all = f.bool();
				words.forEach(word -> all.must(oneWord(f, word)));
				return all;
			})
			.fetchHits(MAX_MATCHES);
	}

	/**
	 * How one typed word may match. Typo forgiveness grows with the word, because a typo in a short word turns it
	 * into a different word ("case" -> "cast") - the lesson from the D1 spike, where "iphnoe" also found "phone":
	 *
	 * <pre>
	 *   1-4 letters: exact word, or the start of a word ("silic" -> silicone)
	 *   5-7 letters: + 1 typo ("iphnoe" -> iphone: two letters swapped counts as ONE typo)
	 *   8+  letters: + 2 typos
	 *   the first letter must always be right (so "iphnoe" can never become "phones")
	 * </pre>
	 * An exact word scores 4x a typo match, and the name counts more than the brand, the brand more than the rest.
	 */
	static SearchPredicate oneWord(SearchPredicateFactory f, String word) {
		BooleanPredicateClausesStep<?, ?> either = f.bool();
		either.should(f.match().field("name").boost(3f).field("brand").boost(2f).field("category.name")
			.field("description").boost(0.5f).matching(word).boost(4f));
		if (word.length() >= 2) {
			either.should(f.match().field("name_suggest").boost(2f).field("brand_suggest").matching(word));
		}
		int typos = word.length() >= 8 ? 2 : word.length() >= 5 ? 1 : 0;
		if (typos > 0) {
			either.should(f.match().field("name").boost(3f).field("brand").boost(2f).field("category.name")
				.field("description").boost(0.5f).matching(word).fuzzy(typos, 1));
		}
		return either.toPredicate();
	}

	/** A top category includes its sub-categories (the same rule as the listing); an unknown slug matches nothing. */
	private Predicate<SearchRow> byCategory(String slug) {
		if (slug == null || slug.isBlank()) {
			return r -> true;
		}
		Set<Long> ids = Set.copyOf(categoryService.idsWithChildrenOrEmpty(slug));
		return r -> ids.contains(r.categoryId());
	}

	private static boolean inRange(BigDecimal price, BigDecimal min, BigDecimal max) {
		return price != null && (min == null || price.compareTo(min) >= 0) && (max == null || price.compareTo(max) <= 0);
	}

	/** relevance = keep Lucene's order; price asc / desc; newest first. id breaks ties, so pages never shuffle. */
	private static List<SearchRow> sorted(List<SearchRow> rows, String sort, String dir) {
		Comparator<SearchRow> order = switch (sort == null ? "relevance" : sort) {
			case "relevance" -> null;
			case "price" -> {
				Comparator<SearchRow> byPrice = Comparator.comparing(SearchRow::priceFrom);
				yield ("desc".equals(dir) ? byPrice.reversed() : byPrice).thenComparing(SearchRow::id);
			}
			case "newest" -> Comparator.comparing(SearchRow::createdAt).reversed().thenComparing(SearchRow::id);
			default -> throw new BadRequestException("sort must be relevance, price or newest");
		};
		return order == null ? rows : rows.stream().sorted(order).toList();
	}

	/** Most products first, then A-Z. */
	private static List<ValueCount> brandCounts(List<SearchRow> rows) {
		return rows.stream().collect(Collectors.groupingBy(SearchRow::brand, Collectors.counting())).entrySet().stream()
			.map(e -> new ValueCount(e.getKey(), e.getValue()))
			.sorted(Comparator.comparingLong(ValueCount::count).reversed().thenComparing(ValueCount::value))
			.toList();
	}

	private static List<CategoryCount> categoryCounts(List<SearchRow> rows) {
		Map<String, CategoryCount> bySlug = new LinkedHashMap<>();
		for (SearchRow r : rows) {
			bySlug.merge(r.categorySlug(), new CategoryCount(r.categorySlug(), r.categoryName(), 1),
					(a, b) -> new CategoryCount(a.slug(), a.name(), a.count() + 1));
		}
		return bySlug.values().stream()
			.sorted(Comparator.comparingLong(CategoryCount::count).reversed().thenComparing(CategoryCount::name))
			.toList();
	}

	/** Every band is listed (0 included), so the price list does not jump around between searches. */
	private static List<PriceBand> priceCounts(List<SearchRow> rows) {
		return BANDS.stream()
			.map(b -> new PriceBand(b.label(), b.min(), b.max(),
					rows.stream().filter(r -> inRange(r.priceFrom(), b.min(), b.max())).count()))
			.toList();
	}

}

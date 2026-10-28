package com.trivoko.search;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

/**
 * How Lucene cuts text into words (an "analyzer"). The same analyzer runs on the product names when they are
 * indexed AND on what the customer types, so both sides look the same:
 *
 * <pre>
 *   "Volta V12 5G – Café Edition"  -> standard: [Volta] [V12] [5G] [Café] [Edition]
 *                                   -> lowercase + asciiFolding: [volta] [v12] [5g] [cafe] [edition]
 *   trivoko_autocomplete also keeps every start of a word (edge n-grams, 2-15 letters):
 *                                      volta -> [vo] [vol] [volt] [volta]   (so "vol" finds Volta while typing)
 * </pre>
 *
 * Named in application.yml (hibernate.search.backend.analysis.configurer).
 */
public class SearchAnalysis implements LuceneAnalysisConfigurer {

	/** Normal words: product name, brand, description, category name. */
	public static final String TEXT = "trivoko_text";

	/** Word starts, for suggestions while typing. Searched with TEXT (the typed letters are NOT cut again). */
	public static final String AUTOCOMPLETE = "trivoko_autocomplete";

	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer(TEXT).custom()
			.tokenizer("standard")
			.tokenFilter("lowercase")
			.tokenFilter("asciiFolding");
		context.analyzer(AUTOCOMPLETE).custom()
			.tokenizer("standard")
			.tokenFilter("lowercase")
			.tokenFilter("asciiFolding")
			.tokenFilter("edgeNGram").param("minGramSize", "2").param("maxGramSize", "15");
	}

}

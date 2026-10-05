package com.trivoko.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Turns names into URL parts: "Ravi's Gadget Shop" -> "ravi-s-gadget-shop". */
public final class Slugs {

	private Slugs() {
	}

	public static String slugify(String text, String fallback) {
		String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")                 // é -> e
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-+|-+$)", "");
		return slug.isEmpty() ? fallback : slug;
	}

	/** The slug, or "-2", "-3" ... added until isTaken says no. */
	public static String unique(String text, String fallback, Predicate<String> isTaken) {
		String base = slugify(text, fallback);
		String slug = base;
		for (int n = 2; isTaken.test(slug); n++) {
			slug = base + "-" + n;
		}
		return slug;
	}

}

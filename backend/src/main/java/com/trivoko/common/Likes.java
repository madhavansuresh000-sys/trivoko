package com.trivoko.common;

import java.util.Locale;

/**
 * Turns a search box text into a LIKE pattern for "contains", e.g. "Chennai " -> "%chennai%".
 * % and _ typed by the user are escaped (use ESCAPE '\' in the query), so "100%" means the text "100%".
 * Empty text -> null, and the queries read "null" as "no search".
 */
public final class Likes {

	private Likes() {
	}

	public static String contains(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		String escaped = text.trim().toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}

}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Normalize utilities.
 */
public final class Normalizer {

	private Normalizer() {
		// Factory pattern
	}

	/**
	 * Normalize a collection of string. Order is respected (LinkedHashSet) but not by function contract (Set).
	 *
	 * @param items The human-readable strings
	 * @return the normalized items.
	 */
	public static Set<String> normalize(final Collection<String> items) {
		return normalize(CollectionUtils.emptyIfNull(items).stream());
	}

	/**
	 * Normalize a collection of string. Order is respected (LinkedHashSet) but not by function contract (Set).
	 *
	 * @param items The human-readable strings
	 * @return the normalized items.
	 */
	public static Set<String> normalize(final Stream<String> items) {
		final Set<String> result = new LinkedHashSet<>();
		items.map(Normalizer::normalize).forEach(result::add);
		return result;
	}

	/**
	 * Normalize and trim a string. Lower case, and without diacritical marks.
	 *
	 * @param item The human-readable string. A DN or any LDAP attribute.
	 * @return the normalized and trimmed item.
	 */
	public static String normalize(@NotNull final String item) {
		return java.text.Normalizer.normalize(StringUtils.trimToEmpty(item), Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ENGLISH);
	}

}

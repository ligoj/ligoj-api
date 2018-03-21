/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * Normalizer format to upper case and without diacritical marks.
 */
public class NormalizeFormat extends Format {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
		toAppendTo.append(org.ligoj.app.api.Normalizer.normalize(obj.toString()).toUpperCase(Locale.ENGLISH));
		return toAppendTo;
	}

	@Override
	public Object parseObject(final String source, final ParsePosition pos) {
		return Integer.valueOf(source);
	}

}

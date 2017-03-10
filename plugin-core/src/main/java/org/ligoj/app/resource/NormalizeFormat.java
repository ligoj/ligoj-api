package org.ligoj.app.resource;

import java.text.FieldPosition;
import java.text.Format;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * Normalizer format
 */
public class NormalizeFormat extends Format {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
		toAppendTo.append(Normalizer.normalize(obj.toString(), Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
				.toUpperCase(Locale.ENGLISH));
		return toAppendTo;
	}

	@Override
	public Object parseObject(final String source, final ParsePosition pos) {
		return Integer.valueOf(source);
	}

}

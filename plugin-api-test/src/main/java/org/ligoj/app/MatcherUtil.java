package org.ligoj.app;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.Assertions;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.opentest4j.AssertionFailedError;

/**
 * {@link Matcher} utilities
 */
public class MatcherUtil {

	/**
	 * Generate a {@link Matcher} checking a {@link ValidationJsonException}
	 * error: field and message.
	 * 
	 * @param ex
	 *            The exception to check.
	 * @param field
	 *            the error field name.
	 * @param message
	 *            the unique error message
	 * @return the built matcher.
	 */
	public static void assertThrows(final ValidationJsonException ex, final String field, final String message) {
		final Collection<Map<String, Serializable>> errors = CollectionUtils.emptyIfNull(ex.getErrors().get(field));
		Assertions.assertTrue(errors.stream().anyMatch(e -> message.equals(e.get("rule"))), "Expected " + field + "='" + message
				+ "' but was '" + errors.stream().findFirst().map(e -> e.get("rule")).orElse(null) + "'");
	}

	/**
	 * Generate a {@link Matcher} checking a {@link ValidationJsonException}
	 * error: field and message.
	 * 
	 * @param field
	 *            the error field name.
	 * @param message
	 *            the unique error message
	 * @return the built matcher.
	 */
	public static void assertThrows(final ConstraintViolationException ex, final String field, final String message) {
		String closest = null;
		for (final ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			if (field.equals(violation.getPropertyPath().toString())) {
				closest = violation.getMessageTemplate();
				if (Objects.equals(message, violation.getMessageTemplate()) || message
						.equalsIgnoreCase(ClassUtils.getShortClassName(ClassUtils.getPackageName(violation.getMessageTemplate())))) {
					return;
				}
			}
		}
		throw new AssertionFailedError("Expected " + field + "='" + message + "' but was '" + closest + "'");
	}

}

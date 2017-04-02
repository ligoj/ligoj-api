package org.ligoj.app;

import java.util.Objects;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.ClassUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.rules.ExpectedException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * {@link Matcher} utilities
 */
public class MatcherUtil {

	/**
	 * Generate a {@link Matcher} checking a {@link ValidationJsonException} error: field and message.
	 * 
	 * @param field
	 *            the error field name.
	 * @param message
	 *            the unique error message
	 * @return the built matcher.
	 */
	public static Matcher<ValidationJsonException> validationMatcher(final String field, final String message) {
		return new BaseMatcher<ValidationJsonException>() {

			@Override
			public boolean matches(final Object item) {
				final ValidationJsonException exception = (ValidationJsonException) item;
				return exception.getErrors().get(field) != null
						&& exception.getErrors().get(field).stream().anyMatch(e -> message.equals(e.get("rule")));
			}

			@Override
			public void describeTo(final Description description) {
				description.appendText(field + " : " + message);
			}
		};
	}

	/**
	 * Generate a {@link Matcher} checking a {@link ValidationJsonException} error: field and message.
	 * 
	 * @param field
	 *            the error field name.
	 * @param message
	 *            the unique error message
	 * @return the built matcher.
	 */
	public static Matcher<ConstraintViolationException> constraintMatcher(final String field, final String message) {
		return new BaseMatcher<ConstraintViolationException>() {

			@Override
			public boolean matches(final Object item) {
				final ConstraintViolationException exception = (ConstraintViolationException) item;
				for (final ConstraintViolation<?> violation : exception.getConstraintViolations()) {
					if (field.equals(violation.getPropertyPath().toString()) && (Objects.equals(message, violation.getMessageTemplate())
							|| message.equalsIgnoreCase(ClassUtils.getShortClassName(ClassUtils.getPackageName(violation.getMessageTemplate()))))) {
						return true;
					}
				}
				return false;
			}

			@Override
			public void describeTo(final Description description) {
				description.appendText(field + " : " + message);
			}
		};
	}

	/**
	 * Add a check : {@link ValidationJsonException} exception must be thrown for a field with a message.
	 * 
	 * @param thrown
	 *            Rule manager for exception.
	 * @param field
	 *            the error field name.
	 * @param message
	 *            the unique error message
	 */
	public static void expectValidationException(final ExpectedException thrown, final String field, final String message) {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(validationMatcher(field, message));
	}

}

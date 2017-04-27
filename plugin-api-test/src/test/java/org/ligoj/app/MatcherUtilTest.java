package org.ligoj.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Test class of {@link MatcherUtil}
 */
public class MatcherUtilTest {

	@Test
	public void matchesNotMatch() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
		final Path path = Mockito.mock(Path.class);
		Mockito.when(violation.getPropertyPath()).thenReturn(path);
		Mockito.when(path.toString()).thenReturn("any");
		violations.add(violation);
		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assert.assertFalse(MatcherUtil.constraintMatcher("firstName", "message").matches(violationException));
	}

	@Test
	public void expectValidationException() {
		new MatcherUtil();
		MatcherUtil.expectValidationException(Mockito.mock(ExpectedException.class), "field", "message");
	}

	@Test
	public void describeTo() {
		MatcherUtil.constraintMatcher("firstName", "message").describeTo(newDescription());
	}

	@Test
	public void matches() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
		final Path path = Mockito.mock(Path.class);
		Mockito.when(violation.getPropertyPath()).thenReturn(path);
		Mockito.when(violation.getMessageTemplate()).thenReturn("message");
		Mockito.when(path.toString()).thenReturn("firstName");
		violations.add(violation);

		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assert.assertTrue(MatcherUtil.constraintMatcher("firstName", "message").matches(violationException));
	}

	@Test
	public void matchesMessagePackage() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
		final Path path = Mockito.mock(Path.class);
		Mockito.when(violation.getPropertyPath()).thenReturn(path);
		Mockito.when(violation.getMessageTemplate()).thenReturn("some.Message.error");
		Mockito.when(path.toString()).thenReturn("firstName");
		violations.add(violation);

		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assert.assertTrue(MatcherUtil.constraintMatcher("firstName", "message").matches(violationException));
	}

	@Test
	public void matchesNotExpectedMessage() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
		final Path path = Mockito.mock(Path.class);
		Mockito.when(violation.getPropertyPath()).thenReturn(path);
		Mockito.when(violation.getMessageTemplate()).thenReturn("any");
		Mockito.when(path.toString()).thenReturn("firstName");
		violations.add(violation);

		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assert.assertFalse(MatcherUtil.constraintMatcher("firstName", "message").matches(violationException));
	}

	@Test
	public void validationMatcherDescribeTo() {
		MatcherUtil.validationMatcher("firstName", "message").describeTo(newDescription());
	}

	@Test
	public void validationMatcherMatches() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "message");
		errors.add(error);
		violationException.getErrors().put("firstName", errors);
		Assert.assertTrue(MatcherUtil.validationMatcher("firstName", "message").matches(violationException));
	}

	@Test
	public void validationMatcherMatchesNotExpectedMessage() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "any");
		errors.add(error);
		violationException.getErrors().put("firstName", errors);
		Assert.assertFalse(MatcherUtil.validationMatcher("firstName", "message").matches(violationException));
	}

	@Test
	public void validationMatcherMatchesNoErrorForField() {
		Assert.assertFalse(MatcherUtil.validationMatcher("firstName", "message").matches(new ValidationJsonException()));

	}

	private Description newDescription() {
		Description mock = Mockito.mock(Description.class);
		Mockito.when(mock.appendText(ArgumentMatchers.anyString())).thenReturn(mock);
		return mock;
	}
}

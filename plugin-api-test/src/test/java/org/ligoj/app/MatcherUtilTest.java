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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;

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
		Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		});
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
		MatcherUtil.assertThrows(violationException, "firstName", "message");
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
		MatcherUtil.assertThrows(violationException, "firstName", "message");
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
		Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		});
	}

	@Test
	public void validationMatcherMatches() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "message");
		errors.add(error);
		violationException.getErrors().put("firstName", errors);
		MatcherUtil.assertThrows(violationException, "firstName", "message");
	}

	@Test
	public void validationMatcherMatchesNotExpectedMessage() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "any");
		errors.add(error);
		violationException.getErrors().put("firstName", errors);
		Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		});
	}

	@Test
	public void validationMatcherMatchesNoErrorForField() {
		Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(new ValidationJsonException(), "firstName", "message");
		});
	}
}

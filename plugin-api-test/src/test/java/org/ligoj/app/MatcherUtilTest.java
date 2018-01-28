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
	public void coverage() {
		new MatcherUtil();
	}

	@Test
	public void assertThrowsNotSameField() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
		final Path path = Mockito.mock(Path.class);
		Mockito.when(violation.getPropertyPath()).thenReturn(path);
		Mockito.when(path.toString()).thenReturn("any");
		violations.add(violation);
		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assertions.assertEquals("expected: <firstName> but was: <[any]>", Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		}).getMessage());
	}

	@Test
	public void assertThrowsNoField() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assertions.assertEquals("expected: <firstName> but was: <[]>", Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		}).getMessage());
	}

	@Test
	public void assertThrows() {
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
	public void assertThrowsMessagePackage() {
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
	public void assertThrowsNotSameMessage() {
		final Set<ConstraintViolation<?>> violations = new HashSet<>();
		final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
		final Path path = Mockito.mock(Path.class);
		Mockito.when(path.toString()).thenReturn("firstName");
		Mockito.when(violation.getPropertyPath()).thenReturn(path);
		Mockito.when(violation.getMessageTemplate()).thenReturn("any");
		violations.add(violation);

		final ConstraintViolationException violationException = new ConstraintViolationException(violations);
		Assertions.assertEquals("expected: <message> but was: <any>", Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		}).getMessage());
	}

	@Test
	public void assertThrowsValidation() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "message");
		errors.add(error);
		violationException.getErrors().put("firstName", errors);
		MatcherUtil.assertThrows(violationException, "firstName", "message");
	}

	@Test
	public void assertThrowsValidationNotSameMessage() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "any");
		errors.add(error);
		violationException.getErrors().put("firstName", errors);
		Assertions.assertEquals("expected: <message> but was: <any>", Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		}).getMessage());
	}

	@Test
	public void assertThrowsValidationNotSameProperty() {
		final ValidationJsonException violationException = new ValidationJsonException();
		final List<Map<String, Serializable>> errors = new ArrayList<>();
		final Map<String, Serializable> error = new HashMap<>();
		error.put("rule", "any");
		errors.add(error);
		violationException.getErrors().put("any", errors);
		Assertions.assertEquals("expected: <firstName> but was: <[any]>", Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(violationException, "firstName", "message");
		}).getMessage());
	}

	@Test
	public void assertThrowsValidationNoField() {
		Assertions.assertEquals("expected: <any> but was: <[]>", Assertions.assertThrows(AssertionFailedError.class, () -> {
			MatcherUtil.assertThrows(new ValidationJsonException(), "any", "message");
		}).getMessage());
	}
}

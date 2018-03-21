/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**
 * Test class of {@link Normalizer}
 */
public class NormalizerTest extends AbstractDataGeneratorTest {

	@Test
	public void normalizeSet() {
		final List<String> strings = new ArrayList<>();
		strings.add("c");
		strings.add("C");
		strings.add(" c ");
		final Set<String> result = Normalizer.normalize(strings);
		Assertions.assertEquals(1, result.size());
		Assertions.assertTrue(result.contains("c"));
	}

	@Test
	public void normalizeSetNull() {
		final Set<String> result = Normalizer.normalize((Collection<String>) null);
		Assertions.assertEquals(0, result.size());
	}

	@Test
	public void normalize() {
		Assertions.assertEquals("c", Normalizer.normalize(" C "));
		Assertions.assertEquals("c", Normalizer.normalize("c"));
	}

	@Test
	public void normalizDiacritic() {
		Assertions.assertEquals("c", Normalizer.normalize("ç"));
		Assertions.assertEquals("aaiconeeeuuaaiconeeeuu", Normalizer.normalize("àâîçôñéêèûùÂÀÎÇÔÑÊÉÈÛÙ"));
	}

	@Test
	public void testCoverage() throws SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		coverageSingleton(Normalizer.class);
	}

}

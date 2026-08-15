/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test class of {@link Normalizer}
 */
class NormalizerTest extends AbstractDataGeneratorTest {

	@Test
	void normalizeSet() throws ReflectiveOperationException {
		coverageSingleton(Normalizer.class);

		final List<String> strings = new ArrayList<>();
		strings.add("c");
		strings.add("C");
		strings.add(" c ");
		final var result = Normalizer.normalize(strings);
		Assertions.assertEquals(1, result.size());
		Assertions.assertTrue(result.contains("c"));
	}

	@Test
	void normalizeSetNull() {
		final var result = Normalizer.normalize((Collection<String>) null);
		Assertions.assertEquals(0, result.size());
	}

	@Test
	void normalize() {
		Assertions.assertEquals("c", Normalizer.normalize(" C "));
		Assertions.assertEquals("c", Normalizer.normalize("c"));
	}

	@Test
	void normalizeDiacritic() {
		Assertions.assertEquals("c", Normalizer.normalize("ç"));
		Assertions.assertEquals("aaiconeeeuuaaiconeeeuu", Normalizer.normalize("àâîçôñéêèûùÂÀÎÇÔÑÊÉÈÛÙ"));
	}

}

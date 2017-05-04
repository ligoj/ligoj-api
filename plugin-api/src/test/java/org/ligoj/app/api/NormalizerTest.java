package org.ligoj.app.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
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
		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.contains("c"));
	}

	@Test
	public void normalizeSetNull() {
		final Set<String> result = Normalizer.normalize((Collection<String>) null);
		Assert.assertEquals(0, result.size());
	}

	@Test
	public void normalize() {
		Assert.assertEquals("c", Normalizer.normalize(" C "));
		Assert.assertEquals("c", Normalizer.normalize("c"));
	}

	@Test
	public void normalizDiacritic() {
		Assert.assertEquals("c", Normalizer.normalize("ç"));
		Assert.assertEquals("aaiconeeeuuaaiconeeeuu", Normalizer.normalize("àâîçôñéêèûùÂÀÎÇÔÑÊÉÈÛÙ"));
	}

	@Test
	public void testCoverage() throws SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		coverageSingleton(Normalizer.class);
	}

}

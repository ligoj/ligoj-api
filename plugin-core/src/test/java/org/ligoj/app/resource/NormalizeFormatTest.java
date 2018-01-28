package org.ligoj.app.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link NormalizeFormat} test class
 */
public class NormalizeFormatTest {

	@Test
	public void testFormat() {
		Assertions.assertEquals("oneTWOOAC", new NormalizeFormat().format("twoÖÀç", new StringBuffer("one"), null).toString());
	}

	@Test
	public void testParseObject() {
		Assertions.assertEquals(Integer.valueOf(1), new NormalizeFormat().parseObject("1", null));
	}

}

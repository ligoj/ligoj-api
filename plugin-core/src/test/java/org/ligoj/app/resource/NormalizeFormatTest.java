/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link NormalizeFormat} test class
 */
class NormalizeFormatTest {

	@Test
	void testFormat() {
		Assertions.assertEquals("oneTWOOAC", new NormalizeFormat().format("twoÖÀç", new StringBuffer("one"), null).toString());
	}

	@Test
	void testParseObject() {
		Assertions.assertEquals(Integer.valueOf(1), new NormalizeFormat().parseObject("1", null));
	}

}

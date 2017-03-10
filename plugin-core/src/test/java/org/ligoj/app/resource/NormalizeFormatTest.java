package org.ligoj.app.resource;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link NormalizeFormat} test class
 */
public class NormalizeFormatTest {

	@Test
	public void testFormat() {
		Assert.assertEquals("oneTWOOAC", new NormalizeFormat().format("twoÖÀç", new StringBuffer("one"), null).toString());
	}

	@Test
	public void testParseObject() {
		Assert.assertEquals(Integer.valueOf(1), new NormalizeFormat().parseObject("1", null));
	}

}

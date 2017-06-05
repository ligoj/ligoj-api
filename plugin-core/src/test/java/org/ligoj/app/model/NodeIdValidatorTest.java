package org.ligoj.app.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link NodeIdValidator}
 */
public class NodeIdValidatorTest {

	@Test
	public void invalid() {
		Assert.assertFalse(new NodeIdValidator().isValid("service", null));
		Assert.assertFalse(new NodeIdValidator().isValid("service:", null));
		Assert.assertFalse(new NodeIdValidator().isValid("service:a:", null));
		Assert.assertFalse(new NodeIdValidator().isValid("service:a:B", null));
		Assert.assertFalse(new NodeIdValidator().isValid("1:a:b", null));
		Assert.assertFalse(new NodeIdValidator().isValid("service:a:bé", null));
		Assert.assertFalse(new NodeIdValidator().isValid("service:a:b_c", null));
		Assert.assertFalse(new NodeIdValidator().isValid("", null));
		Assert.assertFalse(new NodeIdValidator().isValid(null, null));
		Assert.assertFalse(new NodeIdValidator().isValid(" ", null));
	}

	@Test
	public void valid() {
		// only there for coverage
		new NodeIdValidator().initialize(null);

		// Real tests
		Assert.assertTrue(new NodeIdValidator().isValid("service:e", null));
		Assert.assertTrue(new NodeIdValidator().isValid("service:a:b", null));
		Assert.assertTrue(new NodeIdValidator().isValid("service:a:b:c:1", null));
	}

}

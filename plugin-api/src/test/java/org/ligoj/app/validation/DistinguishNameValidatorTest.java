package org.ligoj.app.validation;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link DistinguishNameValidator}
 */
public class DistinguishNameValidatorTest {

	@Test
	public void invalid() {
		Assert.assertFalse(new DistinguishNameValidator().isValid("dc==", null));
		Assert.assertFalse(new DistinguishNameValidator().isValid("dc", null));
		Assert.assertFalse(new DistinguishNameValidator().isValid(",dc=com", null));
		Assert.assertFalse(new DistinguishNameValidator().isValid("dc=f¨r", null));
		Assert.assertFalse(new DistinguishNameValidator().isValid(":dc=com", null));
		Assert.assertFalse(new DistinguishNameValidator().isValid("-dc=com", null));
	}

	@Test
	public void valid() {
		// only there for coverage
		new DistinguishNameValidator().initialize(null);

		// Real tests
		Assert.assertTrue(new DistinguishNameValidator().isValid(null, null));
		Assert.assertTrue(new DistinguishNameValidator().isValid("", null));
		Assert.assertTrue(new DistinguishNameValidator().isValid("0dc=com", null));
		Assert.assertTrue(new DistinguishNameValidator().isValid("dc=com", null));
		Assert.assertTrue(new DistinguishNameValidator().isValid("dc=sample,dc=com", null));
		Assert.assertTrue(new DistinguishNameValidator().isValid(" ou  = A , dc=sample,dc =com ", null));
		Assert.assertTrue(new DistinguishNameValidator().isValid(" ou  = 3s34 , dc=sample,dc =com ", null));
		Assert.assertTrue(new DistinguishNameValidator().isValid(" ou  = À:éè ù , dc=g-üfì,dc =com ", null));
	}
}

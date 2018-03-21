/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link DistinguishNameValidator}
 */
public class DistinguishNameValidatorTest {

	@Test
	public void invalid() {
		Assertions.assertFalse(new DistinguishNameValidator().isValid("dc==", null));
		Assertions.assertFalse(new DistinguishNameValidator().isValid("dc", null));
		Assertions.assertFalse(new DistinguishNameValidator().isValid(",dc=com", null));
		Assertions.assertFalse(new DistinguishNameValidator().isValid("dc=f¨r", null));
		Assertions.assertFalse(new DistinguishNameValidator().isValid(":dc=com", null));
		Assertions.assertFalse(new DistinguishNameValidator().isValid("-dc=com", null));
	}

	@Test
	public void valid() {
		// only there for coverage
		new DistinguishNameValidator().initialize(null);

		// Real tests
		Assertions.assertTrue(new DistinguishNameValidator().isValid(null, null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid("", null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid("0dc=com", null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid("dc=com", null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid("dc=sample,dc=com", null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid(" ou  = A , dc=sample,dc =com ", null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid(" ou  = 3s34 , dc=sample,dc =com ", null));
		Assertions.assertTrue(new DistinguishNameValidator().isValid(" ou  = À:éè ù , dc=g-üfì,dc =com ", null));
	}
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.SimpleUser;

/**
 * Test class of {@link SimpleUser}
 */
class SimpleUserTest {

	@Test
	void valid() {
		Assertions.assertTrue("name".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertTrue("name-2".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertTrue("3@name".matches(SimpleUser.USER_PATTERN_WRAPPER));
	}

	@Test
	void invalid() {
		Assertions.assertFalse(" name".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertFalse("-name".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertFalse("@name".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertFalse("name:".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertFalse("name ".matches(SimpleUser.USER_PATTERN_WRAPPER));
		Assertions.assertFalse("Name".matches(SimpleUser.USER_PATTERN_WRAPPER));
	}
}

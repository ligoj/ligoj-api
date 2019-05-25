/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.ContainerOrg;

/**
 * Test class of {@link ContainerOrg}
 */
class ContainerOrgTest {

	@Test
	void valid() {
		Assertions.assertTrue("namE-er:az 12".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertTrue("Name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertTrue("Name 2".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertTrue("3 Name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
	}

	@Test
	void invalid() {
		Assertions.assertFalse(" name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertFalse("-name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertFalse("name--er".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertFalse("name-".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertFalse("name:".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assertions.assertFalse("name ".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
	}
}

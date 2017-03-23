package org.ligoj.app.api;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.ContainerOrg;

/**
 * Test class of {@link ContainerOrg}
 */
public class ContainerOrgTest {

	@Test
	public void valid() {
		Assert.assertTrue("namE-er:az 12".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertTrue("Name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertTrue("Name 2".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertTrue("3 Name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
	}

	@Test
	public void invalid() {
		Assert.assertFalse(" name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertFalse("-name".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertFalse("name--er".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertFalse("name-".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertFalse("name:".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
		Assert.assertFalse("name ".matches(ContainerOrg.NAME_PATTERN_WRAPPER));
	}
}

package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * {@link ParameterResource} test cases.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ParameterResourceTest extends AbstractAppTest {

	@Autowired
	private ParameterResource resource;

	@Before
	public void prepare() throws IOException {
		persistEntities("csv", new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
				StandardCharsets.UTF_8.name());
		persistSystemEntities();
	}


	@Test
	public void getNotProvidedParameters() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt:jira:6", SubscriptionMode.LINK);
		Assert.assertEquals(25, parameters.size());
		final int nonDummyStartIndex = 23;
		Assert.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex).getId());
		Assert.assertFalse("service:bt:jira:pkey", parameters.get(nonDummyStartIndex).isSecured());
		Assert.assertEquals("service:bt:jira:project", parameters.get(nonDummyStartIndex + 1).getId());
	}

	@Test
	public void getNotProvidedParametersTool() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt:jira", SubscriptionMode.LINK);
		Assert.assertEquals(32, parameters.size());
		Assert.assertEquals("c_10", parameters.get(0).getId());
		final int nonDummyStartIndex = 23;
		Assert.assertEquals("service:bt:jira:jdbc-driver", parameters.get(nonDummyStartIndex).getId());
		Assert.assertEquals("service:bt:jira:jdbc-password", parameters.get(nonDummyStartIndex + 1).getId());
		Assert.assertTrue("service:bt:jira:jdbc-password", parameters.get(nonDummyStartIndex+1).isSecured());
		Assert.assertEquals("service:bt:jira:jdbc-url", parameters.get(nonDummyStartIndex + 2).getId());
		Assert.assertEquals("service:bt:jira:jdbc-user", parameters.get(nonDummyStartIndex + 3).getId());

		final ParameterVo projectParameter = parameters.get(nonDummyStartIndex + 4);
		Assert.assertEquals("service:bt:jira:password", projectParameter.getId());
		Assert.assertTrue("service:bt:jira:jdbc-password", projectParameter.isSecured());
		Assert.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex + 5).getId());
		Assert.assertFalse("service:bt:jira:pkey", parameters.get(nonDummyStartIndex+5).isSecured());
		final ParameterVo projectParameter2 = parameters.get(nonDummyStartIndex + 6);
		Assert.assertEquals(ParameterType.INTEGER, projectParameter2.getType());
		Assert.assertEquals("service:bt:jira", projectParameter2.getOwner().getId());
		Assert.assertEquals("service:bt:jira:project", projectParameter2.getId());
		Assert.assertEquals(1, projectParameter2.getMin().intValue());
		Assert.assertNull(projectParameter2.getMax());
		Assert.assertNull(projectParameter2.getValues());
	}

	@Test
	public void getNotProvidedParametersServiceEmpty() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt", SubscriptionMode.LINK);
		Assert.assertEquals(0, parameters.size());
	}

	@Test
	public void getNotProvidedParametersServiceToService() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id", SubscriptionMode.LINK);
		Assert.assertEquals(2, parameters.size());
		Assert.assertEquals("service:id:group", parameters.get(0).getId());
		Assert.assertEquals("service:id:uid-pattern", parameters.get(1).getId());
	}

	@Test
	public void getNotProvidedParametersServiceToTool() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap", SubscriptionMode.LINK);
		Assert.assertEquals(19, parameters.size());
		final List<String> expected = Arrays.asList("service:id:ldap:base-dn", "service:id:ldap:companies-dn", "service:id:group",
				"service:id:ldap:groups-dn", "service:id:ldap:local-id-attribute", "service:id:ldap:locked-attribute",
				"service:id:ldap:locked-value", "service:id:ldap:password", "service:id:ldap:people-dn",
				"service:id:ldap:people-internal-dn", "service:id:ldap:people-class", "service:id:ldap:quarantine-dn",
				"service:id:ldap:referral", "service:id:ldap:uid-attribute", "service:id:uid-pattern", "service:id:ldap:url",
				"service:id:ldap:company-pattern", "service:id:ldap:department-attribute", "service:id:ldap:user-dn");
		Assert.assertTrue(parameters.stream().map(ParameterVo::getId).allMatch(expected::contains));
	}

	@Test
	public void getNotProvidedParametersServiceToToolCreate() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap", SubscriptionMode.CREATE);
		Assert.assertEquals(21, parameters.size());
		final List<String> expected = Arrays.asList("service:id:ldap:base-dn", "service:id:ldap:companies-dn", "service:id:group",
				"service:id:ldap:groups-dn", "service:id:ldap:local-id-attribute", "service:id:ldap:locked-attribute",
				"service:id:ldap:locked-value", "service:id:ou", "service:id:parent-group", "service:id:ldap:password",
				"service:id:ldap:people-dn", "service:id:ldap:people-internal-dn", "service:id:ldap:people-class",
				"service:id:ldap:quarantine-dn", "service:id:ldap:referral", "service:id:ldap:uid-attribute", "service:id:uid-pattern",
				"service:id:ldap:url", "service:id:ldap:department-attribute", "service:id:ldap:company-pattern",
				"service:id:ldap:user-dn");
		Assert.assertTrue(parameters.stream().map(ParameterVo::getId).allMatch(expected::contains));
	}

	@Test
	public void getNotProvidedParametersServiceToNode() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap:dig", SubscriptionMode.LINK);
		Assert.assertEquals(1, parameters.size());
		Assert.assertEquals("service:id:group", parameters.get(0).getId());
	}

}

package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * {@link ParameterResource} test cases.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class ParameterResourceTest extends AbstractAppTest {

	@Autowired
	private ParameterResource resource;

	@Autowired
	private ParameterRepository repository;

	@BeforeEach
	public void prepare() throws IOException {
		persistEntities("csv",
				new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
				StandardCharsets.UTF_8.name());
		persistSystemEntities();
	}

	@Test
	public void getNotProvidedParameters() {
		repository.findOneExpected("service:bt:jira:pkey").setDefaultValue("DEFAULT_VALUE1");
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt:jira:6",
				SubscriptionMode.LINK);
		Assertions.assertEquals(25, parameters.size());
		final int nonDummyStartIndex = 23;
		Assertions.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex).getId());
		Assertions.assertEquals("DEFAULT_VALUE1", parameters.get(nonDummyStartIndex).getDefaultValue());
		Assertions.assertFalse(parameters.get(nonDummyStartIndex).isSecured());
		Assertions.assertEquals("service:bt:jira:project", parameters.get(nonDummyStartIndex + 1).getId());
		Assertions.assertNull(parameters.get(nonDummyStartIndex + 1).getDefaultValue());
	}

	@Test
	public void getNotProvidedParametersTool() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt:jira",
				SubscriptionMode.LINK);
		Assertions.assertEquals(32, parameters.size());
		Assertions.assertEquals("c_10", parameters.get(0).getId());
		final int nonDummyStartIndex = 23;
		Assertions.assertEquals("service:bt:jira:jdbc-driver", parameters.get(nonDummyStartIndex).getId());
		Assertions.assertEquals("service:bt:jira:jdbc-password", parameters.get(nonDummyStartIndex + 1).getId());
		Assertions.assertTrue(parameters.get(nonDummyStartIndex + 1).isSecured());
		Assertions.assertEquals("service:bt:jira:jdbc-url", parameters.get(nonDummyStartIndex + 2).getId());
		Assertions.assertEquals("service:bt:jira:jdbc-user", parameters.get(nonDummyStartIndex + 3).getId());

		final ParameterVo projectParameter = parameters.get(nonDummyStartIndex + 4);
		Assertions.assertEquals("service:bt:jira:password", projectParameter.getId());
		Assertions.assertTrue(projectParameter.isSecured());
		Assertions.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex + 5).getId());
		Assertions.assertFalse(parameters.get(nonDummyStartIndex + 5).isSecured());
		final ParameterVo projectParameter2 = parameters.get(nonDummyStartIndex + 6);
		Assertions.assertEquals(ParameterType.INTEGER, projectParameter2.getType());
		Assertions.assertEquals("service:bt:jira", projectParameter2.getOwner().getId());
		Assertions.assertEquals("service:bt:jira:project", projectParameter2.getId());
		Assertions.assertEquals(1, projectParameter2.getMin().intValue());
		Assertions.assertNull(projectParameter2.getMax());
		Assertions.assertNull(projectParameter2.getValues());
	}

	@Test
	public void getNotProvidedParametersServiceEmpty() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt", SubscriptionMode.LINK);
		Assertions.assertEquals(0, parameters.size());
	}

	@Test
	public void getNotProvidedParametersServiceToService() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id", SubscriptionMode.LINK);
		Assertions.assertEquals(2, parameters.size());
		Assertions.assertEquals("service:id:group", parameters.get(0).getId());
		Assertions.assertEquals("service:id:uid-pattern", parameters.get(1).getId());
	}

	@Test
	public void getNotProvidedParametersServiceToTool() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap",
				SubscriptionMode.LINK);
		Assertions.assertEquals(19, parameters.size());
		final List<String> expected = Arrays.asList("service:id:ldap:base-dn", "service:id:ldap:companies-dn",
				"service:id:group", "service:id:ldap:groups-dn", "service:id:ldap:local-id-attribute",
				"service:id:ldap:locked-attribute", "service:id:ldap:locked-value", "service:id:ldap:password",
				"service:id:ldap:people-dn", "service:id:ldap:people-internal-dn", "service:id:ldap:people-class",
				"service:id:ldap:quarantine-dn", "service:id:ldap:referral", "service:id:ldap:uid-attribute",
				"service:id:uid-pattern", "service:id:ldap:url", "service:id:ldap:company-pattern",
				"service:id:ldap:department-attribute", "service:id:ldap:user-dn");
		Assertions.assertTrue(parameters.stream().map(ParameterVo::getId).allMatch(expected::contains));
	}

	@Test
	public void getNotProvidedParametersServiceToToolCreate() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap",
				SubscriptionMode.CREATE);
		Assertions.assertEquals(21, parameters.size());
		final List<String> expected = Arrays.asList("service:id:ldap:base-dn", "service:id:ldap:companies-dn",
				"service:id:group", "service:id:ldap:groups-dn", "service:id:ldap:local-id-attribute",
				"service:id:ldap:locked-attribute", "service:id:ldap:locked-value", "service:id:ou",
				"service:id:parent-group", "service:id:ldap:password", "service:id:ldap:people-dn",
				"service:id:ldap:people-internal-dn", "service:id:ldap:people-class", "service:id:ldap:quarantine-dn",
				"service:id:ldap:referral", "service:id:ldap:uid-attribute", "service:id:uid-pattern",
				"service:id:ldap:url", "service:id:ldap:department-attribute", "service:id:ldap:company-pattern",
				"service:id:ldap:user-dn");
		Assertions.assertTrue(parameters.stream().map(ParameterVo::getId).allMatch(expected::contains));
	}

	@Test
	public void getNotProvidedParametersServiceToNode() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap:dig",
				SubscriptionMode.LINK);
		Assertions.assertEquals(1, parameters.size());
		Assertions.assertEquals("service:id:group", parameters.get(0).getId());
	}

}

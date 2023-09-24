/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

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
class ParameterResourceTest extends AbstractAppTest {

	@Autowired
	private ParameterResource resource;

	@Autowired
	private ParameterRepository repository;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv",
				new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
				StandardCharsets.UTF_8);
		persistSystemEntities();
	}

	@Test
	void getNotProvidedParameters() {
		repository.findOneExpected("service:bt:jira:pkey").setDefaultValue("DEFAULT_VALUE1");
		final var parameters = resource.getNotProvidedParameters("service:bt:jira:6", SubscriptionMode.LINK);
		Assertions.assertEquals(25, parameters.size());
		final var nonDummyStartIndex = 23;
		Assertions.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex).getId());
		Assertions.assertEquals("DEFAULT_VALUE1", parameters.get(nonDummyStartIndex).getDefaultValue());
		Assertions.assertFalse(parameters.get(nonDummyStartIndex).isSecured());
		Assertions.assertEquals("service:bt:jira:project", parameters.get(nonDummyStartIndex + 1).getId());
		Assertions.assertNull(parameters.get(nonDummyStartIndex + 1).getDefaultValue());
	}

	@Test
	void getNotProvidedParametersWithDependencies() {

		// c_16->[c_10], c_10->[c_15, c8,c2], c8->c2, c2->[c7], c7->[c14]
		repository.findOneExpected("c_16").getDepends().add(repository.findOneExpected("c_10"));
		repository.findOneExpected("c_10").getDepends().add(repository.findOneExpected("c_15"));
		repository.findOneExpected("c_10").getDepends().add(repository.findOneExpected("c_8"));
		repository.findOneExpected("c_10").getDepends().add(repository.findOneExpected("c_2"));
		repository.findOneExpected("c_8").getDepends().add(repository.findOneExpected("c_2"));
		repository.findOneExpected("c_2").getDepends().add(repository.findOneExpected("c_7"));
		repository.findOneExpected("c_7").getDepends().add(repository.findOneExpected("c_14"));
		final var parameters = resource.getNotProvidedParameters("service:bt:jira:6", SubscriptionMode.LINK);

		// Dependency order check
		Assertions.assertTrue(indexOf("c_16", parameters) > indexOf("c_10", parameters));
		Assertions.assertTrue(indexOf("c_10", parameters) > indexOf("c_15", parameters));
		Assertions.assertTrue(indexOf("c_10", parameters) > indexOf("c_8", parameters));
		Assertions.assertTrue(indexOf("c_10", parameters) > indexOf("c_2", parameters));
		Assertions.assertTrue(indexOf("c_8", parameters) > indexOf("c_2", parameters));
		Assertions.assertTrue(indexOf("c_7", parameters) > indexOf("c_14", parameters));
		Assertions.assertTrue(indexOf("c_2", parameters) > indexOf("c_7", parameters));
		Assertions.assertTrue(indexOf("c_8", parameters) > indexOf("c_2", parameters));

		// Natural order check
		Assertions.assertTrue(indexOf("c_12", parameters) > indexOf("c_11", parameters));
		Assertions.assertTrue(indexOf("c_18", parameters) > indexOf("c_17", parameters));

		// Coverage only
		new Parameter().setDepends(Collections.emptyList());
	}

	private int indexOf(final String parameter, final List<ParameterVo> result) {
		return IntStream.range(0, result.size()).filter(idx -> result.get(idx).getId().equals(parameter)).findFirst()
				.getAsInt();
	}

	@Test
	void getNotProvidedParametersTool() {
		final var parameters = resource.getNotProvidedParameters("service:bt:jira", SubscriptionMode.LINK);
		Assertions.assertEquals(32, parameters.size());
		Assertions.assertEquals("c_10", parameters.get(0).getId());
		final var nonDummyStartIndex = 23;
		Assertions.assertEquals("service:bt:jira:jdbc-driver", parameters.get(nonDummyStartIndex).getId());
		Assertions.assertEquals("service:bt:jira:jdbc-password", parameters.get(nonDummyStartIndex + 1).getId());
		Assertions.assertTrue(parameters.get(nonDummyStartIndex + 1).isSecured());
		Assertions.assertEquals("service:bt:jira:jdbc-url", parameters.get(nonDummyStartIndex + 2).getId());
		Assertions.assertEquals("service:bt:jira:jdbc-user", parameters.get(nonDummyStartIndex + 3).getId());

		final var projectParameter = parameters.get(nonDummyStartIndex + 4);
		Assertions.assertEquals("service:bt:jira:password", projectParameter.getId());
		Assertions.assertTrue(projectParameter.isSecured());
		Assertions.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex + 5).getId());
		Assertions.assertFalse(parameters.get(nonDummyStartIndex + 5).isSecured());
		final var projectParameter2 = parameters.get(nonDummyStartIndex + 6);
		Assertions.assertEquals(ParameterType.INTEGER, projectParameter2.getType());
		Assertions.assertEquals("service:bt:jira", projectParameter2.getOwner().getId());
		Assertions.assertEquals("service:bt:jira:project", projectParameter2.getId());
		Assertions.assertEquals(1, projectParameter2.getMin().intValue());
		Assertions.assertNull(projectParameter2.getMax());
		Assertions.assertNull(projectParameter2.getValues());
	}

	@Test
	void getNotProvidedParametersServiceEmpty() {
		final var parameters = resource.getNotProvidedParameters("service:bt", SubscriptionMode.LINK);
		Assertions.assertEquals(0, parameters.size());
	}

	@Test
	void getNotProvidedParametersServiceToService() {
		final var parameters = resource.getNotProvidedParameters("service:id", SubscriptionMode.LINK);
		Assertions.assertEquals(2, parameters.size());
		Assertions.assertEquals("service:id:group", parameters.get(0).getId());
		Assertions.assertEquals("service:id:uid-pattern", parameters.get(1).getId());
	}

	@Test
	void getNotProvidedParametersServiceToTool() {
		final var parameters = resource.getNotProvidedParameters("service:id:ldap", SubscriptionMode.LINK);
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
	void getNotProvidedParametersServiceToToolCreate() {
		final var parameters = resource.getNotProvidedParameters("service:id:ldap", SubscriptionMode.CREATE);
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
	void findByIdInternalNotExists() {
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.findByIdInternal("not-exists"));
	}

	@Test
	void findByIdInternalNotVisible() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class,
				() -> resource.findByIdInternal("service:id:ldap:base-dn"));
	}

	@Test
	void findByIdInternal() {
		Assertions.assertEquals("service:id:ldap:base-dn",
				resource.findByIdInternal("service:id:ldap:base-dn").getId());
	}

	@Test
	void getNotProvidedParametersServiceToNode() {
		final var parameters = resource.getNotProvidedParameters("service:id:ldap:dig", SubscriptionMode.LINK);
		Assertions.assertEquals(1, parameters.size());
		Assertions.assertEquals("service:id:group", parameters.get(0).getId());
	}

}

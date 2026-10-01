/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test class of {@link SubscriptionRepository}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class SubscriptionRepositoryTest extends AbstractAppTest {

	@Autowired
	private SubscriptionRepository repository;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class }, StandardCharsets.UTF_8);
	}

	private Set<String> toPairs(final List<Object[]> rows) {
		return rows.stream().map(r -> ((Subscription) r[0]).getId() + "/" + ((ParameterValue) r[1]).getId())
				.collect(Collectors.toSet());
	}

	/**
	 * The expected subscription/value pairs of a project: the non-secured values of each subscription, of its node and
	 * of the parents of its node.
	 */
	private Set<String> expected(final int project) {
		return repository.findAllByProject(project).stream()
				.flatMap(s -> parameterValueRepository.findAllSecureBySubscription(s.getId()).stream()
						.map(v -> s.getId() + "/" + v.getId()))
				.collect(Collectors.toSet());
	}

	private ParameterValue newValue(final String node, final Parameter parameter, final String data) {
		final var value = new ParameterValue();
		value.setNode(em.find(Node.class, node));
		value.setParameter(parameter);
		value.setData(data);
		em.persist(value);
		return value;
	}

	@Test
	void findAllWithValuesSecureByProject() {
		// Same subscription/value pairs as the subscription lookups, for each project
		var total = 0;
		for (final var project : em.createQuery("SELECT id FROM Project", Integer.class).getResultList()) {
			final var actual = repository.findAllWithValuesSecureByProject(project);
			Assertions.assertEquals(expected(project), toPairs(actual), "project " + project);
			total += actual.size();
		}
		Assertions.assertTrue(total > 0);
	}

	@Test
	void findAllWithValuesSecureByProjectInheritedValues() {
		final var subscription = em
				.createQuery("FROM Subscription s WHERE s.node.id = 'service:bt:jira:6'", Subscription.class)
				.setMaxResults(1).getSingleResult();
		final var parameter = em.createQuery("FROM Parameter WHERE secured = false", Parameter.class).setMaxResults(1)
				.getSingleResult();
		final var secured = em.createQuery("FROM Parameter WHERE secured = true", Parameter.class).setMaxResults(1)
				.getSingleResult();

		// Values of the subscribed node and its parents: inherited
		final var onInstance = newValue("service:bt:jira:6", parameter, "instance");
		final var onTool = newValue("service:bt:jira", parameter, "tool");
		final var onService = newValue("service:bt", parameter, "service");

		// Not inherited: a sibling node, a secured value
		final var onSibling = newValue("service:bt:jira:4", parameter, "sibling");
		final var onToolSecured = newValue("service:bt:jira", secured, "secret");
		em.flush();

		final var pairs = toPairs(repository.findAllWithValuesSecureByProject(subscription.getProject().getId()));
		Assertions.assertTrue(pairs.contains(subscription.getId() + "/" + onInstance.getId()));
		Assertions.assertTrue(pairs.contains(subscription.getId() + "/" + onTool.getId()));
		Assertions.assertTrue(pairs.contains(subscription.getId() + "/" + onService.getId()));
		Assertions.assertFalse(pairs.contains(subscription.getId() + "/" + onSibling.getId()));
		Assertions.assertFalse(pairs.contains(subscription.getId() + "/" + onToolSecured.getId()));
		Assertions.assertEquals(expected(subscription.getProject().getId()), pairs);
	}
}

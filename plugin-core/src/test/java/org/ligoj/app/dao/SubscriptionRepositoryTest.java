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

	/**
	 * The former single query: a cross join of the subscriptions and all the parameter values.
	 */
	private static final String CROSS_JOIN = "SELECT s, p FROM Subscription s, ParameterValue p INNER JOIN FETCH s.node service LEFT JOIN p.subscription subscription INNER JOIN FETCH p.parameter param "
			+ " LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE s.project.id = :project AND (subscription.id = s.id OR  n0.id = service.id OR n1.refined.id = service.id OR n2.refined.id = service.id) AND param.secured != TRUE";

	@Autowired
	private SubscriptionRepository repository;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class }, StandardCharsets.UTF_8);
	}

	private Set<String> toPairs(final List<Object[]> rows) {
		return rows.stream().map(r -> ((Subscription) r[0]).getId() + "/" + ((ParameterValue) r[1]).getId())
				.collect(Collectors.toSet());
	}

	@Test
	void findAllWithValuesSecureByProject() {
		// Same subscription/value pairs as the former cross join, for each project
		var total = 0;
		for (final var project : em.createQuery("SELECT id FROM Project", Integer.class).getResultList()) {
			@SuppressWarnings("unchecked")
			final List<Object[]> expected = em.createQuery(CROSS_JOIN).setParameter("project", project).getResultList();
			final var actual = repository.findAllWithValuesSecureByProject(project);
			Assertions.assertEquals(toPairs(expected), toPairs(actual), "project " + project);
			Assertions.assertEquals(expected.size(), actual.size());
			total += actual.size();
		}
		Assertions.assertTrue(total > 0);
	}

	@Test
	void findAllWithValuesSecureByProjectParentValue() {
		// A value of the subscribed node
		final var subscription = em.createQuery("FROM Subscription", Subscription.class).setMaxResults(1).getSingleResult();
		final var value = new ParameterValue();
		value.setNode(subscription.getNode());
		value.setParameter(em.createQuery("FROM Parameter WHERE secured = false", Parameter.class).setMaxResults(1)
				.getSingleResult());
		value.setData("node-value");
		em.persist(value);
		em.flush();
		final var project = subscription.getProject().getId();
		@SuppressWarnings("unchecked")
		final List<Object[]> expected = em.createQuery(CROSS_JOIN).setParameter("project", project).getResultList();
		Assertions.assertTrue(toPairs(expected).contains(subscription.getId() + "/" + value.getId()));
		Assertions.assertEquals(toPairs(expected), toPairs(repository.findAllWithValuesSecureByProject(project)));
	}
}

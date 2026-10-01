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
 * Test class of {@link ParameterValueRepository}: the inherited values are found by the identifiers of the ancestor
 * nodes, with the same result as the former LIKE patterns.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class ParameterValueRepositoryTest extends AbstractAppTest {

	private static final String RELATED = "  (s.id = :subscription AND (v.subscription.id = :subscription OR v.node.id = s.node.id OR s.node.id LIKE CONCAT(v.node.id, ':%')))";

	@Autowired
	private ParameterValueRepository repository;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class }, StandardCharsets.UTF_8);

		// An inherited value, on a tool node
		final var value = new ParameterValue();
		value.setNode(em.find(Node.class, "service:bt:jira"));
		value.setParameter(em.find(Parameter.class, "service:bt:jira:url"));
		value.setData("http://inherited");
		em.persist(value);
		em.flush();
	}

	private Set<Integer> ids(final List<ParameterValue> values) {
		return values.stream().map(ParameterValue::getId).collect(Collectors.toSet());
	}

	@Test
	void toAncestors() {
		Assertions.assertEquals(List.of("service:bt:jira:6", "service:bt:jira", "service:bt", "service"),
				ParameterValueRepository.toAncestors("service:bt:jira:6"));
		Assertions.assertEquals(List.of(), ParameterValueRepository.toAncestors(null));
	}

	@Test
	void getParameterValues() {
		var total = 0;
		for (final var node : em.createQuery("SELECT id FROM Node", String.class).getResultList()) {
			final var expected = em.createQuery(
					"SELECT v FROM ParameterValue v INNER JOIN v.node n WHERE n.id = :node OR :node LIKE CONCAT(n.id, ':%')",
					ParameterValue.class).setParameter("node", node).getResultList();
			final var actual = repository.getParameterValues(node);
			Assertions.assertEquals(ids(expected), ids(actual), node);
			total += actual.size();
		}
		Assertions.assertTrue(total > 0);
	}

	@Test
	void bySubscription() {
		var total = 0;
		var singles = 0;
		for (final var subscription : em.createQuery("SELECT id FROM Subscription", Integer.class).getResultList()) {
			final var all = em.createQuery("SELECT v FROM ParameterValue v, Subscription s WHERE " + RELATED,
					ParameterValue.class).setParameter("subscription", subscription).getResultList();
			Assertions.assertEquals(ids(all), ids(repository.findAllBySubscription(subscription)), "all " + subscription);

			final var secure = em.createQuery(
					"SELECT v FROM ParameterValue v, Subscription s INNER JOIN FETCH v.parameter AS param WHERE param.secured != TRUE AND "
							+ RELATED, ParameterValue.class).setParameter("subscription", subscription).getResultList();
			Assertions.assertEquals(ids(secure), ids(repository.findAllSecureBySubscription(subscription)),
					"secure " + subscription);

			for (final var parameter : all.stream().map(v -> v.getParameter().getId()).distinct().toList()) {
				final var values = em.createQuery("SELECT v.data FROM ParameterValue v, Subscription s WHERE v.parameter.id = :parameter AND "
						+ RELATED, String.class).setParameter("subscription", subscription).setParameter("parameter", parameter)
						.getResultList();
				if (values.size() == 1) {
					Assertions.assertEquals(values.getFirst(), repository.getSubscriptionParameterValue(subscription, parameter), parameter);
					singles++;
				}
			}
			total += all.size();
		}
		Assertions.assertTrue(total > 0);
		Assertions.assertTrue(singles > 0);
	}

	@Test
	void bySubscriptionNotFound() {
		Assertions.assertEquals(0, repository.findAllBySubscription(-1).size());
		Assertions.assertEquals(0, repository.findAllSecureBySubscription(-1).size());
		Assertions.assertNull(repository.getSubscriptionParameterValue(-1, "service:bt:jira:url"));
	}

	@Test
	void getParameterValuesNull() {
		Assertions.assertEquals(0, repository.getParameterValues(null).size());
	}
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.resource.AbstractOrgTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ligoj.app.model.Project;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Test class of {@link ProjectRepository}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class ProjectRepositoryTest extends AbstractOrgTest {

	@Autowired
	private ProjectRepository repository;

	@Test
	void isVisible() {
		// Same visibility as the fetching query, without loading the project
		final var projects = em.createQuery("SELECT id FROM Project", Integer.class).getResultList();
		var visible = 0;
		for (final var user : List.of(DEFAULT_USER, "fdaugan", "admin-test", "user1", "any")) {
			initSpringSecurityContext(user);
			for (final var project : projects) {
				final var expected = repository.findOneVisible(project, user) != null;
				Assertions.assertEquals(expected, repository.isVisible(project, user), user + "/" + project);
				visible += expected ? 1 : 0;
			}
		}
		Assertions.assertTrue(visible > 0);
		Assertions.assertTrue(visible < projects.size() * 5);
	}

	@Test
	void findAllLight() {
		// Same projects as the visibility check, with the subscription count of each one
		final var projects = em.createQuery("SELECT id FROM Project", Integer.class).getResultList();
		final Map<Integer, Long> counts = em
				.createQuery("SELECT p.id, COUNT(s.id) FROM Project p LEFT JOIN p.subscriptions s GROUP BY p.id", Object[].class)
				.getResultList().stream().collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));
		var total = 0;
		for (final var user : List.of(DEFAULT_USER, "fdaugan", "admin-test", "user1", "any")) {
			initSpringSecurityContext(user);
			final var expected = projects.stream().filter(p -> repository.isVisible(p, user)).collect(Collectors.toSet());
			final var page = repository.findAllLight(user, "", PageRequest.of(0, 1000));
			Assertions.assertEquals(expected.size(), page.getTotalElements(), user);
			final var actual = page.getContent().stream()
					.collect(Collectors.toMap(r -> ((Project) r[0]).getId(), r -> (Long) r[1]));
			Assertions.assertEquals(expected, actual.keySet(), user);
			actual.forEach((p, c) -> Assertions.assertEquals(counts.get(p), c, user + "/" + p));
			total += actual.size();
		}
		Assertions.assertTrue(total > 0);
	}

	@Test
	void findAllLightPaginatedSorted() {
		initSpringSecurityContext(DEFAULT_USER);
		final var sort = JpaSort.unsafe(Sort.Direction.DESC, "COUNT(s)");
		final var all = repository.findAllLight(DEFAULT_USER, "", PageRequest.of(0, 1000, sort)).getContent();
		Assertions.assertFalse(all.isEmpty());
		for (var i = 1; i < all.size(); i++) {
			Assertions.assertTrue((Long) all.get(i - 1)[1] >= (Long) all.get(i)[1]);
		}
		final var page = repository.findAllLight(DEFAULT_USER, "", PageRequest.of(0, 1, sort));
		Assertions.assertEquals(1, page.getContent().size());
		Assertions.assertEquals(all.size(), page.getTotalElements());
	}
}

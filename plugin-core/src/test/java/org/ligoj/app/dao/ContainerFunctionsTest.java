/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.resource.AbstractOrgTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test the <code>inGroup2</code> and <code>inCompany2</code> functions of {@link SecuritySpringDataListener} against
 * the IAM cache data: a user is in a container when it belongs to this container or to one of its sub-containers.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class ContainerFunctionsTest extends AbstractOrgTest {

	/**
	 * Indicate the DN equals the container DN or is under it.
	 */
	private boolean isUnder(final String dn, final String container) {
		return dn.equalsIgnoreCase(container) || dn.toLowerCase().endsWith("," + container.toLowerCase());
	}

	private Set<String> actual(final String function, final String container) {
		return new HashSet<>(em.createQuery("SELECT CONCAT(u.id, '/', c.id) FROM CacheUser u, " + container + " c WHERE "
				+ function + "(u.id, c.id) = true", String.class).getResultList());
	}

	@Test
	void inGroup2() {
		final var groups = em.createQuery("FROM CacheGroup", CacheGroup.class).getResultList();
		final var memberships = em.createQuery("FROM CacheMembership WHERE user IS NOT NULL", CacheMembership.class)
				.getResultList();
		final var expected = new HashSet<String>();
		final var direct = new HashSet<String>();
		for (final var membership : memberships) {
			direct.add(membership.getUser().getId() + "/" + membership.getGroup().getId());
			for (final var group : groups) {
				if (isUnder(membership.getGroup().getDescription(), group.getDescription())) {
					expected.add(membership.getUser().getId() + "/" + group.getId());
				}
			}
		}

		// Some users are only members of a sub-group of the targeted group
		Assertions.assertTrue(expected.size() > direct.size());
		Assertions.assertEquals(expected, actual("inGroup2", "CacheGroup"));
	}

	@Test
	void inCompany2() {
		final var companies = em.createQuery("FROM CacheCompany", CacheCompany.class).getResultList();
		final var users = em.createQuery("FROM CacheUser WHERE company IS NOT NULL", CacheUser.class).getResultList();
		final var expected = new HashSet<String>();
		for (final var user : users) {
			for (final var company : companies) {
				if (isUnder(user.getCompany().getDescription(), company.getDescription())) {
					expected.add(user.getId() + "/" + company.getId());
				}
			}
		}

		// Some users are only members of a sub-company of the targeted company
		final var direct = users.stream().map(u -> u.getId() + "/" + u.getCompany().getId()).collect(Collectors.toSet());
		Assertions.assertTrue(expected.size() > direct.size());
		Assertions.assertEquals(expected, actual("inCompany2", "CacheCompany"));
	}
}

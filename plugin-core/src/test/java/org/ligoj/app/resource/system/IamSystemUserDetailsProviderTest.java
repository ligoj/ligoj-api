/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.system;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.bootstrap.model.system.SystemRole;
import org.ligoj.bootstrap.model.system.SystemRoleAssignment;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.ligoj.bootstrap.resource.system.user.UserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class of {@link IamSystemUserDetailsProvider}: end-to-end through the bootstrap {@link UserResource} lookup,
 * checking the system users are enriched with — and searchable by — their IAM cache attributes.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class IamSystemUserDetailsProviderTest extends AbstractOrgTest {

	@Autowired
	private UserResource resource;

	@BeforeEach
	void setUpSystemUsers() {
		// In addition to DEFAULT_USER ("junit", no IAM entry, role "some") from
		// persistSystemEntities(), add two system users having an IAM cache entry
		final var fdaugan = new SystemUser();
		fdaugan.setLogin("fdaugan");
		em.persist(fdaugan);
		final var mtuyer = new SystemUser();
		mtuyer.setLogin("mtuyer");
		em.persist(mtuyer);
		final var assignment = new SystemRoleAssignment();
		assignment.setRole(em.createQuery("FROM SystemRole WHERE name = 'some'", SystemRole.class).getSingleResult());
		assignment.setUser(fdaugan);
		em.persist(assignment);
		em.flush();
		em.clear();
	}

	@Test
	void findAll() {
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), null);
		Assertions.assertEquals(3, result.getRecordsTotal());

		// IAM-enriched user with a role
		final var fdaugan = result.getData().getFirst();
		Assertions.assertEquals("fdaugan", fdaugan.getLogin());
		Assertions.assertEquals("Fabrice", fdaugan.getFirstName());
		Assertions.assertEquals("Daugan", fdaugan.getLastName());
		Assertions.assertEquals(List.of("fabrice.daugan@sample.com"), fdaugan.getMails());
		Assertions.assertEquals(1, fdaugan.getRoles().size());
		Assertions.assertEquals("some", fdaugan.getRoles().getFirst().getName());
		Assertions.assertNotNull(fdaugan.getRoles().getFirst().getId());

		// System user without IAM entry
		final var junit = result.getData().get(1);
		Assertions.assertEquals("junit", junit.getLogin());
		Assertions.assertNull(junit.getFirstName());
		Assertions.assertNull(junit.getLastName());
		Assertions.assertNull(junit.getMails());
		Assertions.assertEquals(1, junit.getRoles().size());

		// IAM-enriched user without mail nor role
		final var mtuyer = result.getData().get(2);
		Assertions.assertEquals("mtuyer", mtuyer.getLogin());
		Assertions.assertEquals("Marcel", mtuyer.getFirstName());
		Assertions.assertEquals("User", mtuyer.getLastName());
		Assertions.assertEquals(List.of(), mtuyer.getMails());
		Assertions.assertEquals(List.of(), mtuyer.getRoles());
	}

	@Test
	void findAllByMail() {
		// Other IAM users share this mail (fdaugana, fdauganb) but are not system users
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "daugan@sample");
		Assertions.assertEquals(1, result.getRecordsTotal());
		Assertions.assertEquals("fdaugan", result.getData().getFirst().getLogin());
	}

	@Test
	void findAllByFirstName() {
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "ARCEL");
		Assertions.assertEquals(1, result.getRecordsTotal());
		Assertions.assertEquals("mtuyer", result.getData().getFirst().getLogin());
	}

	@Test
	void findAllByLastName() {
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "Daugan");
		Assertions.assertEquals(1, result.getRecordsTotal());
		Assertions.assertEquals("fdaugan", result.getData().getFirst().getLogin());
	}

	@Test
	void findAllByLoginWithoutIamEntry() {
		// "junit" has no CacheUser entry: only matchable by its login
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "juni");
		Assertions.assertEquals(1, result.getRecordsTotal());
		Assertions.assertEquals("junit", result.getData().getFirst().getLogin());
	}

	@Test
	void findAllNoMatch() {
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "no-match-zzz");
		Assertions.assertEquals(0, result.getRecordsTotal());
	}
}

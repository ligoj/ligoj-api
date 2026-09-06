/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
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

import java.util.List;
import java.util.Map;

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

		// A group of the identity provider named (up to the case) after a system role: its members obtain the role at login
		final var group = new CacheGroup();
		group.setId("hr-api");
		group.setName("hr-api");
		group.setDescription("cn=hr-api,ou=groups,dc=sample,dc=com");
		em.persist(group);
		final var membership = new CacheMembership();
		membership.setUser(em.find(CacheUser.class, "fdaugan"));
		membership.setGroup(group);
		em.persist(membership);
		final var federatedRole = new SystemRole();
		federatedRole.setName("HR-API");
		em.persist(federatedRole);
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
		// Federated role obtained through the "hr-api" group
		Assertions.assertEquals(1, fdaugan.getFederatedRoles().size());
		Assertions.assertEquals("hr-api", fdaugan.getFederatedRoles().getFirst().getId());
		Assertions.assertEquals("HR-API", fdaugan.getFederatedRoles().getFirst().getName());

		// System user without IAM entry
		final var junit = result.getData().get(1);
		Assertions.assertEquals("junit", junit.getLogin());
		Assertions.assertNull(junit.getFirstName());
		Assertions.assertNull(junit.getLastName());
		Assertions.assertNull(junit.getMails());
		Assertions.assertEquals(1, junit.getRoles().size());
		Assertions.assertEquals(List.of(), junit.getFederatedRoles());

		// IAM-enriched user without mail nor role
		final var mtuyer = result.getData().get(2);
		Assertions.assertEquals("mtuyer", mtuyer.getLogin());
		Assertions.assertEquals("Marcel", mtuyer.getFirstName());
		Assertions.assertEquals("User", mtuyer.getLastName());
		Assertions.assertEquals(List.of(), mtuyer.getMails());
		Assertions.assertEquals(List.of(), mtuyer.getRoles());
	}

	@Test
	void toFederatedRoles() {
		final var roles = Map.of("HR-API", new SystemRole(), "ops", new SystemRole(), "Exact", new SystemRole());
		final var result = IamSystemUserDetailsProvider.toFederatedRoles(List.of("hr-api", "OPS", "Exact", "none", "HR-API"), roles);
		Assertions.assertEquals(List.of("hr-api:HR-API", "OPS:ops", "Exact:Exact"),
				result.stream().map(r -> r.getId() + ":" + r.getName()).toList());
	}

	private void assertFindAllBy(String order, String criteria, String expectedResult) {
		final var result = resource.findAllWithRoles(newUriInfoAsc(order), criteria);
		Assertions.assertEquals(1, result.getRecordsTotal());
		Assertions.assertEquals(expectedResult, result.getData().getFirst().getLogin());
	}


	@Test
	void findAllByMail() {
		// Other IAM users share this mail (fdaugana, fdauganb) but are not system users
		assertFindAllBy("login", "daugan@sample", "fdaugan");
	}

	@Test
	void findAllByFirstName() {
		assertFindAllBy("login", "ARCEL", "mtuyer");
	}

	@Test
	void findAllByLastName() {
		assertFindAllBy("login", "Daugan", "fdaugan");
	}

	@Test
	void findAllByLoginWithoutIamEntry() {
		// "junit" has no CacheUser entry: only matchable by its login
		assertFindAllBy("login", "juni", "junit");
	}

	@Test
	void findAllNoMatch() {
		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "no-match-zzz");
		Assertions.assertEquals(0, result.getRecordsTotal());
	}

	@Test
	void findAllBlankMailSegmentIgnored() {
		// A blank segment in the comma-separated mails is dropped from the decoration
		final var cacheUser = em.find(org.ligoj.app.iam.model.CacheUser.class, "fdaugan");
		cacheUser.setMails("fabrice.daugan@sample.com, ");
		em.flush();
		em.clear();

		final var result = resource.findAllWithRoles(newUriInfoAsc("login"), "fdauga");
		Assertions.assertEquals(1, result.getRecordsTotal());
		Assertions.assertEquals(List.of("fabrice.daugan@sample.com"), result.getData().getFirst().getMails());
	}
}

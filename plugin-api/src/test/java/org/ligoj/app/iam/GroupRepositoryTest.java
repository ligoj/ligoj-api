/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * Test class of {@link IGroupRepository}
 */
class GroupRepositoryTest {

	@Test
	void findAll() {
		Assertions.assertTrue(new EmptyGroupRepository().findAll().isEmpty());
	}

	@Test
	void delete() {
		new EmptyGroupRepository().delete(null);
	}

	@Test
	void findByDepartment() {
		Assertions.assertNull(new EmptyGroupRepository().findByDepartment("any"));
	}

	@Test
	void create() {
		final var groupLdap = new EmptyGroupRepository().create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", groupLdap.getDn());
		Assertions.assertEquals("Name", groupLdap.getName());
		Assertions.assertEquals("name", groupLdap.getId());
	}

	@Test
	void findByIdExpectedNotExists() {
		final var repository = new EmptyGroupRepository();
		Assertions.assertThrows(ValidationJsonException.class, () -> repository.findByIdExpected("user1", "user2"));
	}

	@Test
	void findByIdExpected() {
		Assertions.assertEquals("user2", new EmptyGroupRepository() {
			@Override
			public GroupOrg findById(String id) {
				return new GroupOrg(id, id, Collections.emptySet());
			}
		}.findByIdExpected("user1", "user2").getId());
	}

}

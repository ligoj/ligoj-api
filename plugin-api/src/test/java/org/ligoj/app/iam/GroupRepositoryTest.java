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
public class GroupRepositoryTest {

	@Test
	public void findAll() {
		Assertions.assertTrue(new EmptyGroupRepository().findAll().isEmpty());
	}

	@Test
	public void delete() {
		new EmptyGroupRepository().delete(null);
	}

	@Test
	public void findByDepartment() {
		Assertions.assertNull(new EmptyGroupRepository().findByDepartment("any"));
	}

	@Test
	public void create() {
		final GroupOrg groupLdap = new EmptyGroupRepository().create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", groupLdap.getDn());
		Assertions.assertEquals("Name", groupLdap.getName());
		Assertions.assertEquals("name", groupLdap.getId());
	}

	@Test
	public void findByIdExpectedNotExists() {
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			new EmptyGroupRepository().findByIdExpected("user1", "user2");
		});
	}

	@Test
	public void findByIdExpected() {
		Assertions.assertEquals("user2", new EmptyGroupRepository() {
			@Override
			public GroupOrg findById(String id) {
				return new GroupOrg(id, id, Collections.emptySet());
			}
		}.findByIdExpected("user1", "user2").getId());
	}

}

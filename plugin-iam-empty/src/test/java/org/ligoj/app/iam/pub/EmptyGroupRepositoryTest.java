/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.pub;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.empty.EmptyGroupRepository;

/**
 * Test class of {@link EmptyGroupRepository}
 */
class EmptyGroupRepositoryTest {

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
	void getTypeName() {
		Assertions.assertEquals("group", new EmptyGroupRepository().getTypeName());
	}

	@Test
	void create() {
		final GroupOrg groupLdap = new EmptyGroupRepository().create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", groupLdap.getDn());
		Assertions.assertEquals("Name", groupLdap.getName());
		Assertions.assertEquals("name", groupLdap.getId());
	}

	@Test
	void findAllPage() {
		Assertions.assertEquals(0, new EmptyGroupRepository().findAll(Collections.emptySet(), null, null, Collections.emptyMap()).getTotalElements());
	}

	@Test
	void addAttributes() {
		new EmptyGroupRepository().addAttributes(null, null, null);
	}

	@Test
	void addGroup() {
		new EmptyGroupRepository().addGroup(null, null);
	}

	@Test
	void empty() {
		new EmptyGroupRepository().empty(null, null);
	}

	@Test
	void addUser() {
		new EmptyGroupRepository().addUser(null, null);
	}

	@Test
	void removeUser() {
		new EmptyGroupRepository().removeUser(null, null);
	}

}

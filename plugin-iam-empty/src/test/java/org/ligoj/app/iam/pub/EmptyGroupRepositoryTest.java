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
public class EmptyGroupRepositoryTest {

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
	public void getTypeName() {
		Assertions.assertEquals("group", new EmptyGroupRepository().getTypeName());
	}

	@Test
	public void create() {
		final GroupOrg groupLdap = new EmptyGroupRepository().create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", groupLdap.getDn());
		Assertions.assertEquals("Name", groupLdap.getName());
		Assertions.assertEquals("name", groupLdap.getId());
	}

	@Test
	public void findAllPage() {
		Assertions.assertEquals(0, new EmptyGroupRepository().findAll(Collections.emptySet(), null, null, Collections.emptyMap()).getTotalElements());
	}

	@Test
	public void addAttributes() {
		new EmptyGroupRepository().addAttributes(null, null, null);
	}

	@Test
	public void addGroup() {
		new EmptyGroupRepository().addGroup(null, null);
	}

	@Test
	public void empty() {
		new EmptyGroupRepository().empty(null, null);
	}

	@Test
	public void addUser() {
		new EmptyGroupRepository().addUser(null, null);
	}

	@Test
	public void removeUser() {
		new EmptyGroupRepository().removeUser(null, null);
	}

}

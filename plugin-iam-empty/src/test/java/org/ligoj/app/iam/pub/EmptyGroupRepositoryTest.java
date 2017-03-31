package org.ligoj.app.iam.pub;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.empty.EmptyGroupRepository;

/**
 * Test class of {@link EmptyGroupRepository}
 */
public class EmptyGroupRepositoryTest {

	@Test
	public void findAll() {
		Assert.assertTrue(new EmptyGroupRepository().findAll().isEmpty());
	}

	@Test
	public void delete() {
		new EmptyGroupRepository().delete(null);
	}

	@Test
	public void findByDepartment() {
		Assert.assertNull(new EmptyGroupRepository().findByDepartment("any"));
	}

	@Test
	public void getTypeName() {
		Assert.assertEquals("group", new EmptyGroupRepository().getTypeName());
	}

	@Test
	public void create() {
		final GroupOrg groupLdap = new EmptyGroupRepository().create("Cn=Some", "Name");
		Assert.assertEquals("Cn=Some", groupLdap.getDn());
		Assert.assertEquals("Name", groupLdap.getName());
		Assert.assertEquals("name", groupLdap.getId());
	}

	@Test
	public void findAllPage() {
		Assert.assertEquals(0, new EmptyGroupRepository().findAll(Collections.emptySet(), null, null, Collections.emptyMap()).getTotalElements());
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

package org.ligoj.app.resource.security;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.UserOrg;

/**
 * Test class of {@link EmptyUserRepository}
 */
public class EmptyUserRepositoryTest {

	@Test
	public void findAll() {
		new EmptyUserRepository().setPassword(null, null);
		Assert.assertTrue(new EmptyUserRepository().findAll().isEmpty());
	}

	@Test
	public void findAll2() {
		Assert.assertTrue(new EmptyUserRepository().findAll(null, null, null, null).getContent().isEmpty());
	}

	@Test
	public void findAllBy() {
		Assert.assertTrue(new EmptyUserRepository().findAllBy("any", "any").isEmpty());
	}

	@Test
	public void authenticate() {
		Assert.assertTrue(new EmptyUserRepository().authenticate("any", "any"));
	}

	@Test
	public void findByIdNoCache() {
		Assert.assertEquals("some", new EmptyUserRepository().findByIdNoCache("some").getId());
	}

	@Test
	public void getToken() {
		Assert.assertEquals("some", new EmptyUserRepository().getToken("some"));
	}

	@Test
	public void getCompanyRepository() {
		Assert.assertNotNull(new EmptyIamProvider().getConfiguration().getUserRepository().getCompanyRepository());
	}

	@Test
	public void getPeopleInternalBaseDn() {
		Assert.assertEquals("", new EmptyUserRepository().getPeopleInternalBaseDn());
	}

	@Test
	public void updateUser() {
		new EmptyUserRepository().updateUser(null);
	}

	@Test
	public void move() {
		new EmptyUserRepository().move(null, null);
	}

	@Test
	public void restore() {
		new EmptyUserRepository().restore(null);
	}

	@Test
	public void unlock() {
		new EmptyUserRepository().unlock(null);
	}

	@Test
	public void isolate() {
		new EmptyUserRepository().isolate(null, null);
	}

	@Test
	public void lock() {
		new EmptyUserRepository().lock(null, null);
	}

	@Test
	public void delete() {
		new EmptyUserRepository().delete(null);
	}

	@Test
	public void updateMembership() {
		new EmptyUserRepository().updateMembership(null, null);
	}

	@Test
	public void create() {
		final UserOrg entry = new UserOrg();
		Assert.assertSame(entry, new EmptyUserRepository().create(entry));
	}

	@Test
	public void toDn() {
		Assert.assertEquals("", new EmptyUserRepository().toDn(null));
	}
}

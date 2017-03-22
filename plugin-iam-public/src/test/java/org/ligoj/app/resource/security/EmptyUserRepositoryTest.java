package org.ligoj.app.resource.security;

import org.junit.Assert;
import org.junit.Test;

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

}

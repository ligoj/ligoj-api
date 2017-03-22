package org.ligoj.app.iam;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.CompanyOrg;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;

/**
 * Test class of {@link IUserRepository}
 */
public class UserRepositoryTest {

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
	public void toUser() {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assert.assertNotNull(configuration);
		Assert.assertNotNull(configuration.getUserRepository());
		Assert.assertEquals("login", configuration.getUserRepository().toUser("login").getId());
	}

	@Test
	public void toUserNull() {
		Assert.assertNull(new EmptyIamProvider().getConfiguration().getUserRepository().toUser(null));
	}

	@Test
	public void toUserNotExists() {
		Assert.assertEquals("login", new MockUserRepository().toUser("login").getId());
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedNotFound() {
		new MockUserRepository().findByIdExpected("user1", "user2");
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedCompanyNotExists() {
		final EmptyUserRepository emptyUserRepository = new EmptyUserRepository();
		emptyUserRepository.setCompanyRepository(Mockito.mock(ICompanyRepository.class));
		emptyUserRepository.findByIdExpected("user1", "user2");
	}

	@Test
	public void findByIdExpected() throws Exception {
		final EmptyUserRepository emptyUserRepository = new EmptyUserRepository();
		final ICompanyRepository companyRepository = Mockito.mock(ICompanyRepository.class);
		Mockito.when(companyRepository.findById("user1", "company")).thenReturn(new CompanyOrg("", ""));
		emptyUserRepository.setCompanyRepository(companyRepository);
		Assert.assertEquals("user2", emptyUserRepository.findByIdExpected("user1", "user2").getId());
	}

}

package org.ligoj.app.iam;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

/**
 * Test class of {@link IamProvider}.
 */
public class IamProviderTest {

	@Test
	public void authenticate() throws Exception {
		final Authentication mock = Mockito.mock(Authentication.class);
		Assert.assertSame(mock, new EmptyIamProvider().authenticate(mock));
	}

	@Test
	public void getConfiguration() throws Exception {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assert.assertNotNull(configuration);
		Assert.assertNotNull(configuration.getCompanyRepository());
		Assert.assertNotNull(configuration.getUserRepository());
		Assert.assertNotNull(configuration.getGroupRepository());
	}

	@Test
	public void getConfigurationFindById() throws Exception {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assert.assertEquals("any", configuration.getUserRepository().findById("any").getId());
		Assert.assertNull(configuration.getGroupRepository().findById("any"));
		Assert.assertNull(configuration.getCompanyRepository().findById("any"));
		Assert.assertNull(new MockUserRepository2().findById("any"));
	}

	@Test
	public void getConfigurationFindByIdExpected() throws Exception {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assert.assertEquals("any", configuration.getUserRepository().findByIdExpected("any").getId());
	}

	@Test(expected = ValidationJsonException.class)
	public void getConfigurationFindByIdDefault() throws Exception {
		Assert.assertNotNull(new MockUserRepository().findByIdExpected("some"));
	}

	@Test
	public void getConfigurationFindOneByDefault() throws Exception {
		Assert.assertNotNull(new MockUserRepository().findOneBy("attribute1", "value1"));
	}
}

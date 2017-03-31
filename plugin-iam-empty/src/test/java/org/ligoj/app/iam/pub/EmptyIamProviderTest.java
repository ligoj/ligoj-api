package org.ligoj.app.iam.pub;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.empty.EmptyIamProvider;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Test class of {@link EmptyIamProvider}.
 */
@Component
public class EmptyIamProviderTest {

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
		Assert.assertEquals("login", configuration.getUserRepository().toUser("login").getId());
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

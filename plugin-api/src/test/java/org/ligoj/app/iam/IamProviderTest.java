package org.ligoj.app.iam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

/**
 * Test class of {@link IamProvider}.
 */
public class IamProviderTest {

	@Test
	public void authenticate() {
		final Authentication mock = Mockito.mock(Authentication.class);
		Assertions.assertSame(mock, new EmptyIamProvider().authenticate(mock));
	}

	@Test
	public void getConfiguration() {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertNotNull(configuration);
		Assertions.assertNotNull(configuration.getCompanyRepository());
		Assertions.assertNotNull(configuration.getUserRepository());
		Assertions.assertNotNull(configuration.getGroupRepository());
	}

	@Test
	public void getConfigurationFindById() {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertEquals("any", configuration.getUserRepository().findById("any").getId());
		Assertions.assertNull(configuration.getGroupRepository().findById("any"));
		Assertions.assertNull(configuration.getCompanyRepository().findById("any"));
		Assertions.assertNull(new MockUserRepository2().findById("any"));
	}

	@Test
	public void getConfigurationFindByIdExpected() {
		final IamConfiguration configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertEquals("any", configuration.getUserRepository().findByIdExpected("any").getId());
	}

	@Test
	public void getConfigurationFindByIdDefault() {
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			Assertions.assertNotNull(new MockUserRepository().findByIdExpected("some"));
		});
	}

	@Test
	public void getConfigurationFindOneByDefault() {
		Assertions.assertNotNull(new MockUserRepository().findOneBy("attribute1", "value1"));
	}
}

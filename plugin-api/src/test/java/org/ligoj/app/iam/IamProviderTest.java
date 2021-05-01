/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

/**
 * Test class of {@link IamProvider}.
 */
class IamProviderTest {

	@Test
	void authenticate() {
		final var mock = Mockito.mock(Authentication.class);
		Assertions.assertSame(mock, new EmptyIamProvider().authenticate(mock));
	}

	@Test
	void getConfiguration() {
		final var configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertNotNull(configuration);
		Assertions.assertNotNull(configuration.getCompanyRepository());
		Assertions.assertNotNull(configuration.getUserRepository());
		Assertions.assertNotNull(configuration.getGroupRepository());
	}

	@Test
	void getConfigurationFindById() {
		final var configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertEquals("any", configuration.getUserRepository().findById("any").getId());
		Assertions.assertNull(configuration.getGroupRepository().findById("any"));
		Assertions.assertNull(configuration.getCompanyRepository().findById("any"));
		Assertions.assertNull(new MockUserRepository2().findById("any"));
	}

	@Test
	void getConfigurationFindByIdExpected() {
		final var configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertEquals("any", configuration.getUserRepository().findByIdExpected("any").getId());
	}

	@Test
	void getConfigurationFindByIdDefault() {
		final var repository = new MockUserRepository();
		Assertions.assertThrows(ValidationJsonException.class, () -> repository.findByIdExpected("some"));
	}

	@Test
	void getConfigurationFindOneByDefault() {
		Assertions.assertNotNull(new MockUserRepository().findOneBy("attribute1", "value1"));
	}
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.pub;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.empty.EmptyIamProvider;
import org.ligoj.app.iam.empty.IamEmptyCache;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.config.CacheConfig;

/**
 * Test class of {@link EmptyIamProvider}.
 */
class EmptyIamProviderTest {

	private final EmptyIamProvider provider = new EmptyIamProvider();

	@Test
	void testIamEmptyCache() {
		@SuppressWarnings("unchecked")
		final Function<String, CacheConfig<?, ?>> provider = Mockito.mock(Function.class);
		new IamEmptyCache().onCreate(Mockito.mock(HazelcastCacheManager.class), provider);
	}

	@Test
	void authenticate() {
		final var mock = Mockito.mock(Authentication.class);
		Assertions.assertSame(mock, provider.authenticate(mock));
	}

	@Test
	void getConfiguration() {
		final var configuration = provider.getConfiguration();
		Assertions.assertNotNull(configuration);
		Assertions.assertNotNull(configuration.getCompanyRepository());
		Assertions.assertNotNull(configuration.getUserRepository());
		Assertions.assertNotNull(configuration.getGroupRepository());
		Assertions.assertEquals("login", configuration.getUserRepository().toUser("login").getId());
	}

	@Test
	void getConfigurationFindById() {
		final var configuration = provider.getConfiguration();
		Assertions.assertEquals("any", configuration.getUserRepository().findById("any").getId());
		Assertions.assertNull(configuration.getGroupRepository().findById("any"));
		Assertions.assertNull(configuration.getCompanyRepository().findById("any"));
		Assertions.assertNull(new MockUserRepository2().findById("any"));
	}

	@Test
	void getConfigurationFindByIdExpected() {
		final var configuration = provider.getConfiguration();
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

	@Test
	void getKey() {
		Assertions.assertEquals("feature:iam:empty", provider.getKey());
	}
}

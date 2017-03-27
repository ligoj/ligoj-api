package org.ligoj.app.resource.security;

import javax.cache.annotation.CacheResult;

import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Default, and empty {@link IamProvider}.
 */
@Component
public class EmptyIamProvider implements IamProvider {

	@Override
	public Authentication authenticate(final Authentication authentication) {
		return authentication;
	}

	@Override
	@CacheResult(cacheName = "iam-public-configuration")
	public IamConfiguration getConfiguration() {
		final IamConfiguration configuration = new IamConfiguration();
		final EmptyCompanyRepository companyRepository = new EmptyCompanyRepository();
		configuration.setCompanyRepository(companyRepository);
		configuration.setGroupRepository(new EmptyGroupRepository());
		final EmptyUserRepository userRepository = new EmptyUserRepository();
		configuration.setUserRepository(userRepository);

		// Also link user/company repositories
		userRepository.setCompanyRepository(companyRepository);
		return configuration;
	}

}

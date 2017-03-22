package org.ligoj.app.iam;

import javax.cache.annotation.CacheResult;

import org.springframework.security.core.Authentication;

/**
 * Default, and empty {@link IamProvider}.
 */
public class EmptyIamProvider implements IamProvider {

	@Override
	public Authentication authenticate(final Authentication authentication) {
		return authentication;
	}

	@Override
	@CacheResult(cacheName = "iam-node-configuration")
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

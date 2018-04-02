/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import javax.cache.annotation.CacheResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

/**
 * Default, and empty {@link IamProvider}.
 */
public class EmptyIamProvider implements IamProvider {

	private IamConfiguration iamConfiguration;

	@Autowired
	private EmptyIamProvider self = this;

	@Override
	public Authentication authenticate(final Authentication authentication) {
		return authentication;
	}

	@Override
	public IamConfiguration getConfiguration() {
		self.refreshConfiguration();
		return getCachedConfiguration();
	}

	public IamConfiguration getCachedConfiguration() {
		return iamConfiguration;
	}

	@CacheResult(cacheName = "iam-test-configuration")
	public String refreshConfiguration() {
		final IamConfiguration configuration = new IamConfiguration();
		final EmptyCompanyRepository companyRepository = new EmptyCompanyRepository();
		configuration.setCompanyRepository(companyRepository);
		configuration.setGroupRepository(new EmptyGroupRepository());
		final EmptyUserRepository userRepository = new EmptyUserRepository();
		configuration.setUserRepository(userRepository);

		// Also link user/company repositories
		userRepository.setCompanyRepository(companyRepository);
		this.iamConfiguration = configuration;
		return "OK";
	}

}

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
	@CacheResult(cacheName = "iam-node-configuration")
	public IamConfiguration getConfiguration() {
		final IamConfiguration configuration = new IamConfiguration();
		configuration.setUserRepository(new EmptyUserRepository());
		configuration.setCompanyRepository(new EmptyCompanyRepository());
		configuration.setGroupRepository(new EmptyGroupRepository());
		return configuration;
	}

}

package org.ligoj.app.resource.security;

import javax.cache.annotation.CacheResult;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import org.ligoj.app.api.SimpleUser;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;

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
		configuration.setToUser(login -> {
			final SimpleUser user = new SimpleUser();
			user.setId(login);
			return user;
		});
		return configuration;
	}

}

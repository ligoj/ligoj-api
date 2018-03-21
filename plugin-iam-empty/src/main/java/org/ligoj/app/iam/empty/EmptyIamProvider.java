/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.empty;

import java.util.Optional;

import javax.cache.annotation.CacheResult;

import org.ligoj.app.api.FeaturePlugin;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Default, and empty {@link IamProvider}.
 */
@Component
@Order(100)
public class EmptyIamProvider implements IamProvider, FeaturePlugin {

	private IamConfiguration iamConfiguration;

	@Autowired
	private EmptyIamProvider self;

	@Override
	public Authentication authenticate(final Authentication authentication) {
		return authentication;
	}

	@Override
	public IamConfiguration getConfiguration() {
		self.refreshConfiguration();
		return Optional.ofNullable(iamConfiguration).orElseGet(this::refreshConfiguration);
	}

	@CacheResult(cacheName = "iam-empty-configuration")
	public boolean ensureCachedConfiguration() {
		refreshConfiguration();
		return true;
	}

	private IamConfiguration refreshConfiguration() {
		final IamConfiguration configuration = new IamConfiguration();
		final EmptyCompanyRepository companyRepository = new EmptyCompanyRepository();
		configuration.setCompanyRepository(companyRepository);
		configuration.setGroupRepository(new EmptyGroupRepository());
		final EmptyUserRepository userRepository = new EmptyUserRepository();
		configuration.setUserRepository(userRepository);

		// Also link user/company repositories
		userRepository.setCompanyRepository(companyRepository);
		this.iamConfiguration = configuration;
		return configuration;
	}

	@Override
	public String getKey() {
		return "feature:iam:empty";
	}

}

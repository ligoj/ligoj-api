/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.empty;

import java.util.Optional;

import javax.cache.annotation.CacheResult;

import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.bootstrap.core.plugin.FeaturePlugin;
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

	private EmptyIamProvider self = this;

	@Override
	public Authentication authenticate(final Authentication authentication) {
		return authentication;
	}

	@Override
	public IamConfiguration getConfiguration() {
		self.ensureCachedConfiguration();
		return Optional.ofNullable(iamConfiguration).orElseGet(this::refreshConfiguration);
	}

	/**
	 * Refresh the configuration.
	 *
	 * @return Ignored.
	 */
	@CacheResult(cacheName = "iam-empty-configuration")
	public boolean ensureCachedConfiguration() {
		refreshConfiguration();
		return true;
	}

	private IamConfiguration refreshConfiguration() {
		final var configuration = new IamConfiguration();
		final var companyRepository = new EmptyCompanyRepository();
		configuration.setCompanyRepository(companyRepository);
		configuration.setGroupRepository(new EmptyGroupRepository());
		final var userRepository = new EmptyUserRepository();
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

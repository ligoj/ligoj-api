package org.ligoj.app.resource.plugin;

import java.io.Serializable;

import javax.persistence.EntityNotFoundException;

import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.Configurable;
import org.ligoj.app.model.PluginConfiguration;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base implementation of a configurable {@link ServicePlugin} without action.
 */
public abstract class AbstractConfiguredServicePlugin<C extends PluginConfiguration> extends AbstractServicePlugin implements ConfigurablePlugin {

	@Autowired
	protected ProjectRepository projectRepository;

	@Autowired
	protected SecurityHelper securityHelper;

	/**
	 * Check the visibility of a configured entity.
	 * 
	 * @param entity
	 *            The requested configured entity.
	 * @return The formal entity parameter.
	 */
	private <K extends Serializable, T extends Configurable<C, K>> T checkConfiguredVisibility(final T configured) {
		final Subscription entity = subscriptionRepository.findOneExpected(configured.getConfiguration().getSubscription().getId());
		if (projectRepository.findOneVisible(entity.getProject().getId(), securityHelper.getLogin()) == null) {
			// Associated project is not visible, reject the configuration access
			throw new EntityNotFoundException(configured.getId().toString());
		}
		return configured;
	}

	/**
	 * Check the visibility of a configured entity.
	 * 
	 * @param id
	 *            The requested configured identifier.
	 * @return The entity where the related subscription if visible.
	 */
	protected <K extends Serializable, T extends Configurable<C, K>> T findConfigured(final RestRepository<T, K> repository, final K id) {
		return checkConfiguredVisibility(repository.findOneExpected(id));
	}

	/**
	 * Delete the configured entity if the related subscription is visible.
	 * 
	 * @param id
	 *            The requested configured identifier.
	 */
	protected <K extends Serializable, T extends Configurable<C, K>> void deletedConfigured(final RestRepository<T, K> repository, final K id) {
		repository.delete(checkConfiguredVisibility(repository.findOneExpected(id)));
	}

}

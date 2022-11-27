/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.Serializable;

import javax.persistence.EntityNotFoundException;

import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.api.NodeScoped;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.Configurable;
import org.ligoj.app.model.PluginConfiguration;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base implementation of a configurable {@link ServicePlugin} without action.
 *
 * @param <C> The configuration entity type.
 */
public abstract class AbstractConfiguredServicePlugin<C extends PluginConfiguration> extends AbstractServicePlugin
		implements ConfigurablePlugin {

	@Autowired
	protected ProjectRepository projectRepository;

	@Autowired
	protected SecurityHelper securityHelper;

	/**
	 * Check the visibility of a configured entity.
	 *
	 * @param configured The requested configured entity.
	 * @param <K>        The {@link Configurable} identifier type.
	 * @param <T>        The {@link Configurable} type.
	 * @return The formal entity parameter.
	 */
	protected <K extends Serializable, T extends Configurable<C, K>> T checkConfiguredVisibility(final T configured) {
		final var entity = subscriptionRepository
				.findOneExpected(configured.getConfiguration().getSubscription().getId());
		if (projectRepository.findOneVisible(entity.getProject().getId(), securityHelper.getLogin()) == null) {
			// Associated project is not visible, reject the configuration access
			throw new EntityNotFoundException(configured.getId().toString());
		}
		return configured;
	}

	/**
	 * Check the visibility of a configured entity.
	 *
	 * @param repository The repository holding the configured entity.
	 * @param id         The requested configured identifier.
	 * @param <K>        The {@link Configurable} identifier type.
	 * @param <T>        The {@link Configurable} type.
	 * @return The entity where the related subscription if visible.
	 */
	public <K extends Serializable, T extends Configurable<C, K>> T findConfigured(
			final RestRepository<T, K> repository, final K id) {
		return checkConfiguredVisibility(repository.findOneExpected(id));
	}

	/**
	 * Check the visibility of a configured entity by its name.
	 *
	 * @param repository   The repository holding the configured entity.
	 * @param name         The requested configured entity's name.
	 * @param subscription The required subscription owner.
	 * @param <K>          The {@link Configurable} identifier type.
	 * @param <T>          The {@link Configurable} type.
	 * @return The entity where the related subscription if visible.
	 * @since 2.1.1
	 */
	public <K extends Serializable, T extends Configurable<C, K>> T findConfiguredByName(
			final RestRepository<T, K> repository, final String name, final int subscription) {
		return checkConfiguredVisibility(
				repository.findAllBy("configuration.subscription.id", subscription, new String[] { "name" }, name)
						.stream().findFirst().orElseThrow(() -> new EntityNotFoundException(name)));
	}

	/**
	 * Check the node scoped object is related to the given node. Will fail with a {@link EntityNotFoundException} if
	 * the related node if not a sub node of the required node.
	 *
	 * @param nodeScoped   The object related to a node.
	 * @param requiredNode The widest accepted node relationship.
	 * @param <T>          The {@link Configurable} type.
	 * @return the formal node coped object when the visibility has been checked.
	 */
	public <T extends NodeScoped<?>> T checkVisibility(final T nodeScoped, final String requiredNode) {
		// Compare the node against the scoped entity
		if (!nodeScoped.getNode().getId().matches("^" + requiredNode + "(:.+)?$")) {
			// The expected node does not exist in the expected node scope
			throw new EntityNotFoundException(nodeScoped.getId().toString());
		}

		// Checked
		return nodeScoped;
	}

	/**
	 * Delete the configured entity if the related subscription is visible.
	 *
	 * @param repository The repository holding the configured entity.
	 * @param <K>        The {@link Configurable} identifier type.
	 * @param <T>        The {@link Configurable} type.
	 * @param id         The requested configured identifier.
	 */
	public <K extends Serializable, T extends Configurable<C, K>> void deletedConfigured(
			final RestRepository<T, K> repository, final K id) {
		repository.delete(checkConfiguredVisibility(repository.findOneExpected(id)));
	}

}

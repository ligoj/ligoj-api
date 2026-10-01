/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.Serializable;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.ForbiddenException;

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

	/**
	 * Project repository, used to check the visibility and the management of the related projects.
	 */
	@Autowired
	protected ProjectRepository projectRepository;

	/**
	 * Security helper, used to resolve the current user.
	 */
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
	 * Return a configured entity the current user can modify: the related subscription is visible, and the
	 * subscriptions of its project are managed by the current user. To use before any update of the configuration,
	 * {@link #findConfigured(RestRepository, Serializable)} only checks the visibility.
	 *
	 * @param repository The repository holding the configured entity.
	 * @param id         The requested configured identifier.
	 * @param <K>        The {@link Configurable} identifier type.
	 * @param <T>        The {@link Configurable} type.
	 * @return The entity where the related subscription is managed.
	 * @throws EntityNotFoundException When the configured entity or its subscription is not visible.
	 * @throws ForbiddenException      When the related subscription is visible but not managed.
	 */
	public <K extends Serializable, T extends Configurable<C, K>> T findConfiguredManaged(
			final RestRepository<T, K> repository, final K id) {
		final var configured = checkConfiguredVisibility(repository.findOneExpected(id));
		final var project = configured.getConfiguration().getSubscription().getProject().getId();
		if (!projectRepository.isManageSubscription(project, securityHelper.getLogin())) {
			// Visible, but read only for this user
			throw new ForbiddenException();
		}
		return configured;
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
	 * the related node is not a sub node of the required node.
	 *
	 * @param nodeScoped   The object related to a node.
	 * @param requiredNode The widest accepted node relationship.
	 * @param <T>          The {@link NodeScoped} type.
	 * @return the formal node scoped object when the visibility has been checked.
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
	 * Delete the configured entity if the related subscription is visible and managed by the current user.
	 *
	 * @param repository The repository holding the configured entity.
	 * @param <K>        The {@link Configurable} identifier type.
	 * @param <T>        The {@link Configurable} type.
	 * @param id         The requested configured identifier.
	 */
	public <K extends Serializable, T extends Configurable<C, K>> void deletedConfigured(
			final RestRepository<T, K> repository, final K id) {
		repository.delete(findConfiguredManaged(repository, id));
	}

}

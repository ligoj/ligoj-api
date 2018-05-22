/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.io.Serializable;

import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Persistable;

/**
 * Base class for resource that can be locked.
 *
 * @param <T>
 *            Type of locked resource.
 * @param <I>
 *            Identifier type of locked resource.
 */
public abstract class AbstractLockedResource<T extends Persistable<I>, I extends Serializable> {

	@Autowired
	protected ServicePluginLocator locator;

	@Autowired
	protected ApplicationContext applicationContext;

	/**
	 * Delete all tasks related the given entity and check there is no running tasks.
	 *
	 * @param plugin
	 *            The related resource plug-in managing the entity being deleted.
	 * @param id
	 *            The entity's identifier being deleted.
	 */
	public void deleteTasks(final ServicePlugin plugin, final I id) {
		// Check and delete the related finished tasks
		final String scope = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		applicationContext.getBeansOfType(getLongTaskRunnerClass()).values().stream()
				.filter(l -> l.getClass().getProtectionDomain().getCodeSource().getLocation().toString().equals(scope))
				.forEach(l -> l.deleteTask(id));
	}

	/**
	 * Delete all tasks related the given entity and check there is no running tasks.
	 *
	 * @param node
	 *            The related node's identifier owning the locked resource.
	 * @param id
	 *            The entity's identifier being deleted.
	 * @param deleteRemoteData
	 *            When <code>true</code>, remote data will be also destroyed.
	 * @throws Exception
	 *             When any delete fails. Managed at upper level.
	 */
	public void deleteWithTasks(final String node, final I id, final boolean deleteRemoteData) throws Exception {
		// Delegate the deletion
		ServicePlugin plugin = locator.getResource(node);
		while (plugin != null) {
			// Pre-check for long task
			deleteTasks(plugin, id);

			delete(plugin, id, deleteRemoteData);
			plugin = locator.getResource(locator.getParent(plugin.getKey()));
		}
	}

	/**
	 * Delete a locked resource by its identifier and owned by the given plug-in.
	 *
	 * @param plugin
	 *            The related plug-in owning the locked resource
	 * @param id
	 *            The locked's identifier resource.
	 * @param deleteRemoteData
	 *            When <code>true</code>, remote data will be also destroyed.
	 * @throws Exception
	 *             When delete fails. Managed at upper level.
	 */
	protected abstract void delete(ServicePlugin plugin, final I id, final boolean deleteRemoteData) throws Exception;

	/**
	 * Return the {@link Class} of type {@link LongTaskRunner} to check task deletion.
	 *
	 * @return the {@link LongTaskRunner} class type handling the locked resource type.
	 */
	protected abstract Class<? extends LongTaskRunner<?, ?, ?, I, ?, AbstractLockedResource<T, I>>> getLongTaskRunnerClass();

	/**
	 * Check the given identifier relates to a visible entity.
	 *
	 * @param id
	 *            Entity identifier.
	 * @return The visible entity. Never <code>null</code>.
	 */
	public abstract T checkVisible(final I id);
}

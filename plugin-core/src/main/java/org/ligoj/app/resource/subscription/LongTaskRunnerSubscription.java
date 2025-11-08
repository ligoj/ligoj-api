/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.task.LongTaskSubscriptionRepository;
import org.ligoj.app.model.AbstractLongTaskSubscription;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.core.resource.OnNullReturn404;

/**
 * A resource running some long task. Implementing this interface causes the subscription management checks there is no
 * running task when a deletion is requested. The contract :
 * <ul>
 * <li>At most one task can run per node</li>
 * <li>A subscription cannot be deleted while there is a running attached task</li>
 * <li>A running task is task without "end" date.
 * <li>When a task is started, is will always end.
 * <li>When a task ends, the status (boolean) is always updated.
 * </ul>
 *
 * @param <T> Type of task entity.
 * @param <R> Repository managing the task entity.
 */
public interface LongTaskRunnerSubscription<T extends AbstractLongTaskSubscription, R extends LongTaskSubscriptionRepository<T>>
		extends LongTaskRunner<T, R, Subscription, Integer, SubscriptionRepository, SubscriptionResource> {
	@Override
	default SubscriptionRepository getLockedRepository() {
		return getSubscriptionRepository();
	}

	@Override
	default SubscriptionResource getLockedResource() {
		return getSubscriptionResource();
	}

	/**
	 * Return the {@link SubscriptionRepository}.
	 *
	 * @return The repository used to fetch related subscription entity of a task.
	 */
	SubscriptionRepository getSubscriptionRepository();

	/**
	 * Return the {@link SubscriptionResource}.
	 *
	 * @return The resource used to fetch related subscription entity of a task.
	 */
	SubscriptionResource getSubscriptionResource();

	/**
	 * Return status of the task.
	 *
	 * @param subscription The locked subscription identifier.
	 * @return status of task. May <code>null</code> when there is no previous task.
	 */
	@GET
	@Path("{subscription:\\d+}/task")
	default T getTask(@PathParam("subscription") final int subscription) {
		checkVisible(subscription);
		return LongTaskRunner.super.getTaskInternal(subscription);
	}

	/**
	 * Cancel (stop) the current task. Synchronous operation, flag the task as failed.
	 *
	 * @param subscription The locked subscription identifier.
	 * @return The ended task if present or <code>null</code>.
	 */
	@DELETE
	@Path("{subscription:\\d+}/task")
	@OnNullReturn404
	default T cancel(@PathParam("subscription") final int subscription) {
		checkVisible(subscription);
		return endTask(subscription, true);
	}

}

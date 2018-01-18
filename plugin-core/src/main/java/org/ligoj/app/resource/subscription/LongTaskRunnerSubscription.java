package org.ligoj.app.resource.subscription;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.task.LongTaskSubscriptionRepository;
import org.ligoj.app.model.AbstractLongTaskSubscription;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.plugin.LongTaskRunner;

/**
 * A resource running some long task. Implementing this interface causes the
 * subscription management checks there is no running task when a deletion is
 * requested. The contract :
 * <ul>
 * <li>At most one task can run per node</li>
 * <li>A subscription cannot be deleted while there is a running attached
 * task</li>
 * <li>A running task is task without "end" date.
 * <li>When a task is started, is will always ends.
 * <li>When a task ends, the status (boolean) is always updated.
 * </ul>
 */
public interface LongTaskRunnerSubscription<T extends AbstractLongTaskSubscription, R extends LongTaskSubscriptionRepository<T>>
		extends LongTaskRunner<T, R, Subscription, Integer, SubscriptionRepository> {
	@Override
	default SubscriptionRepository getLockedRepository() {
		return getSubscriptionRepository();
	}

	/**
	 * Return the {@link SubscriptionRepository}.
	 * 
	 * @return The repository used to fetch related subscription entity of a task.
	 */
	SubscriptionRepository getSubscriptionRepository();

	/**
	 * Return status of import.
	 * 
	 * @param subscription
	 *            The locked subscription identifier.
	 * @return status of import. May <code>null</code> when there is no previous
	 *         task.
	 */
	@Override
	@GET
	@Path("{subscription:\\d+}/task")
	default T getTask(@PathParam("subscription") final Integer subscription) {
		return LongTaskRunner.super.getTask(subscription);
	}

}

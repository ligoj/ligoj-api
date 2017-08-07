package org.ligoj.app.resource.plugin;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.ligoj.app.dao.LongTaskRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

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
public interface LongTaskRunner<T extends AbstractLongTask, R extends LongTaskRepository<T>> {

	/**
	 * Return the task repository.
	 * 
	 * @return The task repository used to fetch and update the task from inner
	 *         transaction.
	 */
	R getTaskRepository();

	/**
	 * Return the {@link SubscriptionRepository}.
	 * 
	 * @return The repository used to fetch related subscription entity of a
	 *         task.
	 */
	SubscriptionRepository getSubscriptionRepository();

	/**
	 * Check there is no running task for given subscription before the
	 * deletion.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 */
	default void deleteTask(final int subscription) {
		// Check there is no running import
		Optional.ofNullable(getTask(subscription)).filter(t -> !t.isFinished()).ifPresent(t -> {
			throw new BusinessException("Running import not finished", t.getAuthor(), t.getStart(), t.getSubscription().getId());
		});

		// We can safely delete the tasks
		getTaskRepository().deleteAllBy("subscription.id", subscription);
	}

	/**
	 * Supplier, on demand new task entity creation. Only bean constructor, no
	 * involved persistence operation.
	 * 
	 * @return The task constructor.
	 */
	Supplier<T> newTask();

	/**
	 * Mark the given subscription as finished import.
	 * 
	 * @param subscription
	 *            The subscription to reset.
	 * @param failed
	 *            The import failed
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default void endTask(final int subscription, final boolean failed) {
		final T task = createAsNeeded(subscription);
		task.setEnd(new java.util.Date());
		task.setFailed(failed);

		// Save now the new state
		getTaskRepository().saveAndFlush(task);
	}

	/**
	 * Return status of import.
	 * 
	 * @param subscription
	 *            the subscription identifier
	 * @return status of import. May <code>null</code> when there is no previous
	 *         task.
	 */
	@GET
	@Path("{subscription:\\d+}/task")
	default T getTask(@PathParam("subscription") final int subscription) {
		return getTaskRepository().findBy("subscription.id", subscription);
	}

	/**
	 * Reset a task entity. No persistence implied.
	 * 
	 * @param task
	 *            The task to reset. Standard fields of {@link AbstractLongTask}
	 *            are already reset.
	 */
	void resetTask(final T task);

	/**
	 * Check there no running import and starts a new task.
	 * 
	 * @param subscription
	 *            the subscription to lock.
	 * @return the locked task with status.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T startTask(final int subscription) {
		synchronized (this) {

			// Check there is no running task on the same node
			Optional.ofNullable(getTaskRepository().findBySameServiceNotFinished(subscription)).ifPresent(t -> {
				// On this service, there is already a running import
				throw new BusinessException("concurrent-task", t.getAuthor(), t.getStart(), t.getSubscription());
			});

			// Build a new task as needed
			final T task = createAsNeeded(subscription);

			// Reset the specific fields
			resetTask(task);

			// Save this entity inside this transaction
			getTaskRepository().saveAndFlush(task);
			return task;
		}
	}

	/**
	 * Get or create a new task associated to given subscription.
	 * 
	 * @param subscription
	 *            The related subscription of the requested task.
	 * @return The task, never <code>null</code>.
	 */
	default T createAsNeeded(final int subscription) {
		final T task = Optional.ofNullable(getTask(subscription)).orElseGet(() -> {
			final T newTask = newTask().get();
			newTask.setSubscription(getSubscriptionRepository().findOneExpected(subscription));
			return newTask;
		});

		// Reset internal fields
		task.setAuthor(SecurityContextHolder.getContext().getAuthentication().getName());
		task.setStart(new Date());
		task.setEnd(null);
		task.setFailed(false);
		return task;
	}

	/**
	 * Move forward the the step of given import status.
	 * 
	 * @param task
	 *            The task to update.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default void nextStep(final T task) {
		nextStepInternal(task);
		getTaskRepository().saveAndFlush(task);
	}

	/**
	 * Move forward the the step of given import status.
	 * 
	 * @param task
	 *            The task to update.
	 */
	default void nextStepInternal(final T task) {
		// Nothing to do by default
	}
}

package org.ligoj.app.resource.plugin;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.PathParam;

import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.task.LongTaskRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.bootstrap.core.DateUtils;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.data.domain.Persistable;

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
 * 
 * @param <L>
 *            Associated type of locked entity.
 * @param <I>
 *            Associated type of locked entity's identifier.
 * @param <A>
 *            Repository managing the associated type of locked entity.
 */
public interface LongTaskRunner<T extends AbstractLongTask<L, I>, R extends LongTaskRepository<T, L, I>, L extends Persistable<I>, I extends Serializable, A extends RestRepository<L, I>> {

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
	 * @return The repository used to fetch related subscription entity of a task.
	 */
	A getLockedRepository();

	/**
	 * Check there is no running task for given subscription before the deletion.
	 * 
	 * @param lockedId
	 *            The locked entity's identifier.
	 */
	default void deleteTask(final I lockedId) {
		// Check there is no running import
		Optional.ofNullable(getTask(lockedId)).filter(t -> !t.isFinished()).ifPresent(t -> {
			throw new BusinessException("Running import not finished", t.getAuthor(), t.getStart(), t.getLocked().getId());
		});

		// We can safely delete the tasks
		getTaskRepository().deleteAllBy("locked.id", lockedId);
	}

	/**
	 * Supplier, on demand new task entity creation. Only bean constructor, no
	 * involved persistence operation.
	 * 
	 * @return The task constructor.
	 */
	Supplier<T> newTask();

	/**
	 * Release the lock on the locked entity by its identifier. The task is
	 * considered as finished.
	 * 
	 * @param lockedId
	 *            The locked entity's identifier.
	 * @param failed
	 *            The task status as resolution of this task.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default void endTask(final I lockedId, final boolean failed) {
		Optional.ofNullable(getTask(lockedId)).ifPresent(task -> {
			task.setEnd(new Date());
			task.setFailed(failed);

			// Save now the new state
			getTaskRepository().saveAndFlush(task);
		});
	}

	/**
	 * Return status of import.
	 * 
	 * @param lockedId
	 *            The locked entity's identifier.
	 * @return status of import. May <code>null</code> when there is no previous
	 *         task.
	 */
	default T getTask(@PathParam("locked") final I lockedId) {
		return getTaskRepository().findBy("locked.id", lockedId);
	}

	/**
	 * Check there no running task within the same scope of the locked's identifier
	 * and starts a new task.
	 * 
	 * @param lockedId
	 *            The locked entity's identifier.
	 * @param initializer
	 *            The function to call while initializing the task.
	 * @return the locked task with status.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T startTask(final I lockedId, final Consumer<T> initializer) {
		synchronized (lockedId) {

			// Check there is no running task on the same node
			Optional.ofNullable(getTaskRepository().findNotFinishedByLocked(lockedId)).ifPresent(t -> {
				// On this service, there is already a running import
				throw new BusinessException("concurrent-task", t.getAuthor(), t.getStart(), t.getLocked());
			});

			// Build a new task as needed
			final T task = createAsNeeded(lockedId);

			// Reset the specific fields
			initializer.accept(task);

			// Save this entity inside this transaction
			return getTaskRepository().saveAndFlush(task);
		}
	}

	/**
	 * Get or create a new task associated to given subscription.
	 * 
	 * @param lockedId
	 *            The locked entity's identifier. The related entity will be locked.
	 * @return The task, never <code>null</code>.
	 */
	default T createAsNeeded(final I lockedId) {
		final T task = Optional.ofNullable(getTask(lockedId)).orElseGet(() -> {
			final T newTask = newTask().get();
			newTask.setLocked(getLockedRepository().findOneExpected(lockedId));
			return newTask;
		});

		// Reset internal fields
		task.setAuthor(SpringUtils.getBean(SecurityHelper.class).getLogin());
		task.setStart(DateUtils.newCalendar().getTime());
		task.setEnd(null);
		task.setFailed(false);
		return task;
	}

	/**
	 * Move forward the next step of given import status.
	 * 
	 * @param task
	 *            The task to update.
	 * @param steper
	 *            The function to call to update the task for this next step.
	 * @return The updated task.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T nextStep(final I lockedId, final Consumer<T> stepper) {
		final T task = Optional.ofNullable(getTask(lockedId)).orElseThrow(() -> new EntityNotFoundException(lockedId.toString()));
		stepper.accept(task);
		getTaskRepository().saveAndFlush(task);
		return task;
	}

}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.ligoj.app.dao.task.LongTaskRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.resource.node.AbstractLockedResource;
import org.ligoj.bootstrap.core.DateUtils;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.data.domain.Persistable;

/**
 * A resource running some long task. Implementing this interface causes the subscription management checks there is no
 * running task when a deletion is requested. The contract :
 * <ul>
 * <li>At most one task can run per node</li>
 * <li>A subscription cannot be deleted while there is a running attached task</li>
 * <li>A running task is task without "end" date.
 * <li>When a task is started, it will always end.
 * <li>When a task ends, the status (boolean) is always updated.
 * </ul>
 *
 * @param <L> Associated type of locked entity.
 * @param <I> Associated type of locked entity's identifier.
 * @param <A> Repository managing the locked entity.
 * @param <S> Resource managing the locked entity.
 * @param <R> Repository managing the task entity.
 * @param <T> Type of task entity.
 */
public interface LongTaskRunner<T extends AbstractLongTask<L, I>, R extends LongTaskRepository<T, L, I>, L extends Persistable<I>, I extends Serializable, A extends RestRepository<L, I>, S extends AbstractLockedResource<L, I>> {

	/**
	 * Return the task repository.
	 *
	 * @return The task repository used to fetch and update the task from inner transaction.
	 */
	R getTaskRepository();

	/**
	 * Return the {@link AbstractLockedResource} managing the locked resource.
	 *
	 * @return The resource used to fetch a related subscription entity of a task.
	 */
	S getLockedResource();

	/**
	 * Return the {@link RestRepository} managing the locked resource.
	 *
	 * @return The repository used to fetch the related subscription entity of a task.
	 */
	A getLockedRepository();

	/**
	 * Check there is no running task for a given subscription before the deletion.
	 *
	 * @param lockedId The locked entity's identifier.
	 */
	default void deleteTask(final I lockedId) {
		// Check there is no running import
		Optional.ofNullable(getTaskInternal(lockedId)).filter(t -> !isFinished(t)).ifPresent(t -> {
			throw new BusinessException("Running import not finished", t.getAuthor(), t.getStart(), lockedId);
		});

		// We can safely delete the tasks
		getTaskRepository().deleteAllBy("locked.id", lockedId);
	}

	/**
	 * Supplier, on demand new task entity creation. Only bean constructor, no involved persistence operation.
	 *
	 * @return The task constructor.
	 */
	Supplier<T> newTask();

	/**
	 * Release the lock on the locked entity by its identifier. The task is considered as finished.
	 *
	 * @param lockedId The locked entity's identifier.
	 * @param failed   The task status as a resolution of this task.
	 * @return The ended task if present.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T endTask(final I lockedId, final boolean failed) {
		// Execute within a real new transaction to ensure commit visibility to the caller
		final var txManager = SpringUtils.getBean(org.springframework.transaction.PlatformTransactionManager.class);
		final var template = new org.springframework.transaction.support.TransactionTemplate(txManager);
		template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> endTaskInternal(lockedId, failed, t -> {
			// Nothing to do by default
		}));
	}


	/**
	 * Release the lock on the locked entity by its identifier. The task is considered as finished.
	 *
	 * @param lockedId  The locked entity's identifier.
	 * @param failed    The task status as resolution of this task.
	 * @param finalizer The function to call while finalizing the task.
	 * @return The ended task if present.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T endTask(final I lockedId, final boolean failed, final Consumer<T> finalizer) {
		// Execute within a real new transaction to ensure commit visibility to caller
		final var txManager = SpringUtils.getBean(org.springframework.transaction.PlatformTransactionManager.class);
		final var template = new org.springframework.transaction.support.TransactionTemplate(txManager);
		template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> endTaskInternal(lockedId, failed, finalizer));
	}

	/**
	 * Internal end task logic without opening a transaction.
	 */
	default T endTaskInternal(final I lockedId, final boolean failed, final Consumer<T> finalizer) {
		return Optional.ofNullable(getTaskInternal(lockedId)).map(task -> {
			checkNotFinished(task);
			task.setEnd(new Date());
			task.setFailed(failed);
			finalizer.accept(task);
			return getTaskRepository().saveAndFlush(task);
		}).orElse(null);
	}

	/**
	 * Return status of import.
	 *
	 * @param lockedId The locked entity's identifier.
	 * @return status of import. May <code>null</code> when there is no previous task.
	 */
	default T getTaskInternal(final I lockedId) {
		return getTaskRepository().findBy("locked.id", lockedId);
	}

	/**
	 * Check there no running task within the same scope of the locked object's identifier and starts a new task.
	 *
	 * @param lockedId    The locked entity's identifier.
	 * @param initializer The function to call while initializing the task.
	 * @return the locked task with status.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T startTask(final I lockedId, final Consumer<T> initializer) {
		// Execute within a real new transaction to ensure commit visibility to the caller
		final var txManager = SpringUtils.getBean(org.springframework.transaction.PlatformTransactionManager.class);
		final var template = new org.springframework.transaction.support.TransactionTemplate(txManager);
		template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> startTaskInternal(lockedId,initializer));
	}

	/**
	 * Check there no running task within the same scope of the locked object's identifier and starts a new task.
	 *
	 * @param lockedId    The locked entity's identifier.
	 * @param initializer The function to call while initializing the task.
	 * @return the locked task with status.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T startTaskInternal(final I lockedId, final Consumer<T> initializer) {
		synchronized (getLockedRepository()) {

			// Check there is no running task on the same node
			Optional.ofNullable(getTaskRepository().findNotFinishedByLocked(lockedId)).ifPresent(t -> {
				throw new BusinessException("concurrent-task", t.getAuthor(), t.getStart(), lockedId);
			});

			// Build a new task as needed
			final var task = createAsNeeded(lockedId);

			// Reset the specific fields
			initializer.accept(task);

			// Save this entity inside this transaction
			return getTaskRepository().saveAndFlush(task);
		}
	}

	/**
	 * When <code>true</code> the task is really finished. Can be overridden to update the real finished state stored in
	 * the database. For sample, a task can be finished from the client side but not fully executed at the server side.
	 *
	 * @param task The not <code>null</code> task to evaluate.
	 * @return <code>true</code> when the task is finished.
	 */
	default boolean isFinished(final T task) {
		return task.isFinished();
	}

	/**
	 * Get or create a new task associated with given subscription.
	 *
	 * @param lockedId The locked entity's identifier. The related entity will be locked.
	 * @return The task, never <code>null</code>.
	 */
	default T createAsNeeded(final I lockedId) {
		final var task = Optional.ofNullable(getTaskInternal(lockedId)).map(t -> {
			// Additional remote check (optional)
			if (!isFinished(t)) {
				// On this service, there is already a running remote task
				throw new BusinessException("concurrent-task-remote", t.getAuthor(), t.getStart(), lockedId);
			}
			return t;
		}).orElseGet(() -> {
			final var newTask = newTask().get();
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
	 * Move forward the next step of the given import status.
	 *
	 * @param lockedId The locked resource identifier.
	 * @param stepper  The function to call to update the task for this next step.
	 * @return The updated task.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	default T nextStep(final I lockedId, final Consumer<T> stepper) {
		// Execute within a real new transaction to ensure commit visibility to caller
		final var txManager = SpringUtils.getBean(org.springframework.transaction.PlatformTransactionManager.class);
		final var template = new org.springframework.transaction.support.TransactionTemplate(txManager);
		template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> {
			final var task = Optional.ofNullable(getTaskInternal(lockedId))
					.orElseThrow(() -> new EntityNotFoundException(lockedId.toString()));
			checkNotFinished(task);
			stepper.accept(task);
			return getTaskRepository().saveAndFlush(task);
		});
	}

	/**
	 * Check the given task is not finished.
	 *
	 * @param task The task to check.
	 */
	default void checkNotFinished(final T task) {
		if (isFinished(task)) {
			// Request a next step but is already finished (canceled?)
			throw new BusinessException("Already finished");
		}
	}

	/**
	 * Check the locked resource is visible by the principal user.
	 *
	 * @param lockedId The resource identifier.
	 */
	default void checkVisible(final I lockedId) {
		getLockedResource().checkVisible(lockedId);
	}
}

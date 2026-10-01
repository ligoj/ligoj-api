/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import java.io.Serializable;
import java.util.List;

import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} repository.
 *
 * @param <T> Type of task entity.
 * @param <L> The locked type during while this task is running.
 * @param <I> The locked object 's identifier type during while this task is running.
 */
@SuppressWarnings("ALL")
@NoRepositoryBean
public interface LongTaskRepository<T extends AbstractLongTask<L, I>, L extends Persistable<I>, I extends Serializable>
		extends RestRepository<T, Integer> {

	/**
	 * Status counters of the tasks aliased <code>i</code>: total, running and failed, as a single row.
	 */
	String STATUS_COUNTS = "SELECT COUNT(i), COALESCE(SUM(CASE WHEN i.end IS NULL THEN 1 ELSE 0 END), 0),"
			+ " COALESCE(SUM(CASE WHEN i.end IS NOT NULL AND i.failed = TRUE THEN 1 ELSE 0 END), 0) FROM #{#entityName} i";

	/**
	 * Status filter of the tasks aliased <code>i</code>, each status being enabled by its own boolean parameter.
	 */
	String STATUS_FILTER = "((:running = TRUE AND i.end IS NULL)"
			+ " OR (:succeeded = TRUE AND i.end IS NOT NULL AND i.failed = FALSE)"
			+ " OR (:failed = TRUE AND i.end IS NOT NULL AND i.failed = TRUE))";

	/**
	 * Status order expression of the tasks aliased <code>i</code>: the status name.
	 */
	String STATUS_ORDER = "(CASE WHEN i.end IS NULL THEN 'RUNNING' WHEN i.failed = TRUE THEN 'FAILED' ELSE 'SUCCEEDED' END)";

	/**
	 * Return the status counters of all the tasks.
	 *
	 * @return A single row: total, running and failed task counts.
	 */
	@Query(STATUS_COUNTS)
	List<Object[]> countByStatus();

	/**
	 * Return a page of the tasks having one of the enabled statuses.
	 *
	 * @param running   When <code>true</code>, the running tasks are included.
	 * @param succeeded When <code>true</code>, the succeeded tasks are included.
	 * @param failed    When <code>true</code>, the failed tasks are included.
	 * @param page      The pagination and the sort. The status sort uses {@link #STATUS_ORDER}.
	 * @return The page of tasks.
	 */
	@Query("SELECT i FROM #{#entityName} i WHERE " + STATUS_FILTER)
	Page<T> findAllByStatus(boolean running, boolean succeeded, boolean failed, Pageable page);

	/**
	 * Return the running task, not yet finished, locking the given entity.
	 *
	 * @param locked The locked entity's identifier.
	 * @return the running task locking the given entity, or <code>null</code> when there is none.
	 */
	@SuppressWarnings("unused")
	T findNotFinishedByLocked(I locked);

	/**
	 * Lock the row of the locked entity until the end of the current transaction, so concurrent task starts on the
	 * same entity are serialized by the database, across all the cluster members. The default implementation does not
	 * lock and returns <code>null</code>: the caller falls back to a JVM lock.
	 *
	 * @param locked The locked entity's identifier.
	 * @return The locked entity, or <code>null</code> when it does not exist or when database locking is not
	 *         supported.
	 */
	default L lockLocked(final I locked) {
		return null;
	}

}

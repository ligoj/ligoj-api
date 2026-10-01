/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import jakarta.persistence.LockModeType;

import java.util.List;

import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} base repository for Subscription.
 *
 * @param <T> Type of task entity.
 */
@SuppressWarnings("ALL")
@NoRepositoryBean
public interface LongTaskSubscriptionRepository<T extends AbstractLongTask<Subscription, Integer>>
		extends LongTaskRepository<T, Subscription, Integer> {

	@Override
	@Query("FROM #{#entityName} t WHERE t.locked.id = :locked AND t.end IS NULL")
	T findNotFinishedByLocked(Integer locked);

	/**
	 * Return all tasks whose locked subscription belongs to a project visible by the given user. Mirror of the node
	 * repository's {@code findAllVisible}, scoped through the subscription's project visibility.
	 *
	 * @param user The current principal user.
	 * @return The visible tasks for the current principal user.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT i FROM #{#entityName} i INNER JOIN i.locked s INNER JOIN s.project p WHERE "
			+ ProjectRepository.VISIBLE_PROJECTS_EXISTS)
	List<T> findAllVisible(String user);

	/**
	 * Return the status counters of the tasks whose locked subscription belongs to a project visible by the given
	 * user.
	 *
	 * @param user The current principal user.
	 * @return A single row: total, running and failed task counts.
	 */
	@Query(STATUS_COUNTS + " INNER JOIN i.locked s INNER JOIN s.project p WHERE " + ProjectRepository.VISIBLE_PROJECTS_EXISTS)
	List<Object[]> countVisibleByStatus(String user);

	/**
	 * Return a page of the tasks whose locked subscription belongs to a project visible by the given user and having
	 * one of the enabled statuses.
	 *
	 * @param user      The current principal user.
	 * @param running   When <code>true</code>, the running tasks are included.
	 * @param succeeded When <code>true</code>, the succeeded tasks are included.
	 * @param failed    When <code>true</code>, the failed tasks are included.
	 * @param page      The pagination and the sort. The status sort uses {@link #STATUS_ORDER}.
	 * @return The page of visible tasks.
	 */
	@Query("SELECT i FROM #{#entityName} i INNER JOIN i.locked s INNER JOIN s.project p WHERE "
			+ ProjectRepository.VISIBLE_PROJECTS_EXISTS + " AND " + STATUS_FILTER)
	Page<T> findAllVisible(String user, boolean running, boolean succeeded, boolean failed, Pageable page);

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Subscription s WHERE s.id = :locked")
	Subscription lockLocked(Integer locked);
}

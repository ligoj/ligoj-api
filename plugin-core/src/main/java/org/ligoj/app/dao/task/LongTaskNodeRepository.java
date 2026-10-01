/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import jakarta.persistence.LockModeType;

import java.util.List;

import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.Node;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} base repository for Node.
 *
 * @param <T> Type of task entity.
 */
@SuppressWarnings("ALL")
@NoRepositoryBean
public interface LongTaskNodeRepository<T extends AbstractLongTask<Node, String>>
		extends LongTaskRepository<T, Node, String> {

	@Override
	@Query("FROM #{#entityName} i WHERE i.locked.id = :node AND i.end IS NULL")
	T findNotFinishedByLocked(String node);

	/**
	 * Return all tasks whose locked node is visible by the given user.
	 *
	 * @param user The current principal user.
	 * @return The tasks of the nodes visible for the current principal user.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT i FROM #{#entityName} i INNER JOIN i.locked AS n WHERE " + NodeRepository.VISIBLE_NODES)
	List<T> findAllVisible(String user);

	/**
	 * Return the status counters of the tasks whose locked node is visible by the given user.
	 *
	 * @param user The current principal user.
	 * @return A single row: total, running and failed task counts.
	 */
	@Query(STATUS_COUNTS + " INNER JOIN i.locked AS n WHERE " + NodeRepository.VISIBLE_NODES)
	List<Object[]> countVisibleByStatus(String user);

	/**
	 * Return a page of the tasks whose locked node is visible by the given user and having one of the enabled
	 * statuses.
	 *
	 * @param user      The current principal user.
	 * @param running   When <code>true</code>, the running tasks are included.
	 * @param succeeded When <code>true</code>, the succeeded tasks are included.
	 * @param failed    When <code>true</code>, the failed tasks are included.
	 * @param page      The pagination and the sort. The status sort uses {@link #STATUS_ORDER}.
	 * @return The page of visible tasks.
	 */
	@Query("SELECT i FROM #{#entityName} i INNER JOIN i.locked AS n WHERE " + NodeRepository.VISIBLE_NODES + " AND "
			+ STATUS_FILTER)
	Page<T> findAllVisible(String user, boolean running, boolean succeeded, boolean failed, Pageable page);

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT n FROM Node n WHERE n.id = :locked")
	Node lockLocked(String locked);
}

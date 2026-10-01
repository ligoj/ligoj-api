/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import jakarta.persistence.LockModeType;

import java.util.List;

import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.Node;
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

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT n FROM Node n WHERE n.id = :locked")
	Node lockLocked(String locked);
}

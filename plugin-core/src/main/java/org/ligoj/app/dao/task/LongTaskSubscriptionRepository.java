/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import java.util.List;

import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.Subscription;
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
	@Query("SELECT DISTINCT i FROM #{#entityName} i INNER JOIN i.locked s INNER JOIN s.project p"
			+ " LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE " + ProjectRepository.VISIBLE_PROJECTS)
	List<T> findAllVisible(String user);

}

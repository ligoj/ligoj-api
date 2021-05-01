/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.Subscription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} base repository for Subscription.
 *
 * @param <T> Type of task entity.
 */
@NoRepositoryBean
public interface LongTaskSubscriptionRepository<T extends AbstractLongTask<Subscription, Integer>>
		extends LongTaskRepository<T, Subscription, Integer> {

	@Override
	@Query("FROM #{#entityName} t WHERE t.locked.id = :locked AND t.end IS NULL")
	T findNotFinishedByLocked(Integer locked);

}

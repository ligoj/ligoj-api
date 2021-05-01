/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao.task;

import java.io.Serializable;

import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} repository.
 *
 * @param <T> Type of task entity.
 * @param <L> The locked type during while this task is running.
 * @param <I> The locked's identifier type during while this task is running.
 */
@NoRepositoryBean
public interface LongTaskRepository<T extends AbstractLongTask<L, I>, L extends Persistable<I>, I extends Serializable>
		extends RestRepository<T, Integer> {

	/**
	 * Return an active task status for the same service than the given one. .
	 *
	 * @param locked The locked entity's identifier.
	 * @return the import status of a given subscription.
	 */
	T findNotFinishedByLocked(I locked);

}

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
 * @param <I> The locked object 's identifier type during while this task is running.
 */
@SuppressWarnings("ALL")
@NoRepositoryBean
public interface LongTaskRepository<T extends AbstractLongTask<L, I>, L extends Persistable<I>, I extends Serializable>
		extends RestRepository<T, Integer> {

	/**
	 * Return the running task, not yet finished, locking the given entity.
	 *
	 * @param locked The locked entity's identifier.
	 * @return the running task locking the given entity, or <code>null</code> when there is none.
	 */
	@SuppressWarnings("unused")
	T findNotFinishedByLocked(I locked);

}

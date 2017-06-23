package org.ligoj.app.dao;

import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} repository.
 */
@NoRepositoryBean
public interface LongTaskRepository<T extends AbstractLongTask> extends RestRepository<T, Integer> {

	/**
	 * Return not finished import status of a given project.
	 * 
	 * @param project
	 *            the project identifier.
	 * @return <code>null</code> or a not finished import status of a given
	 *         subscription.
	 */
	@Query("FROM #{#entityName} i WHERE i.subscription.project.id = :project AND i.end IS NULL")
	T findByProjectNotFinished(int project);

	/**
	 * Return an active import status for the same service than the given one. .
	 * 
	 * @param subscription
	 *            The subscription to delete.
	 * @return the import status of a given subscription.
	 */
	@Query("SELECT i FROM #{#entityName} i, Subscription s WHERE s.id = :subscription AND i.subscription.node = s.node AND i.end IS NULL")
	T findBySameServiceNotFinished(int subscription);

}

package org.ligoj.app.dao.task;

import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.Node;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * {@link AbstractLongTask} base repository for Node.
 */
@NoRepositoryBean
public interface LongTaskNodeRepository<T extends AbstractLongTask<Node, String>> extends LongTaskRepository<T, Node, String> {

	@Override
	@Query("FROM #{#entityName} i WHERE i.locked.id = :node AND i.end IS NULL")
	T findNotFinishedByLocked(String node);

}

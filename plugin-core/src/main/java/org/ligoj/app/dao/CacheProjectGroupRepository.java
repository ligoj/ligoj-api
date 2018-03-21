/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.model.CacheProjectGroup;
import org.ligoj.app.model.Project;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link CacheProjectGroup} repository
 */
public interface CacheProjectGroupRepository extends RestRepository<CacheProjectGroup, Integer> {

	/**
	 * Return all couples {@link Project} and subscribed group.
	 * 
	 * @return all couples {@link Project} and subscribed group identifier.
	 */
	@Query("SELECT p.id, pv.data FROM ParameterValue pv INNER JOIN pv.subscription AS s"
			+ " INNER JOIN s.project AS p WHERE pv.parameter.id = 'service:id:group'")
	List<Object[]> findAllProjectGroup();
}

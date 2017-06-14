package org.ligoj.app.dao;

import org.ligoj.app.model.Parameter;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Parameter} repository
 */
public interface ParameterRepository extends RestRepository<Parameter, String> {

	/**
	 * Return the parameter with the given identifier and associated to a
	 * visible node by the given user.
	 * 
	 * @param id
	 *            The parameter identifier.
	 * @param user
	 *            The user principal requesting this parameter.
	 * @return The visible parameter or <code>null</code> when not found.
	 */
	@Query("FROM Parameter p INNER JOIN p.owner n WHERE p.id=:id AND " + NodeRepository.VISIBLE_NODES)
	Parameter findOneVisible(String id, String user);
}

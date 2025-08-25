/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.model.Parameter;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Parameter} repository
 */
@SuppressWarnings("ALL")
public interface ParameterRepository extends RestRepository<Parameter, String> {

	/**
	 * Return all parameters associated to a node but without specified value for the given node or its parent. The
	 * parameters are filtered by the requested mode. Also check the node is visible for the given user.
	 *
	 * @param node The parent node identifier.
	 * @param mode Expected mode.
	 * @param user The user requesting the nodes.
	 * @return all parameters associated to a node but without a value.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT DISTINCT p FROM Parameter p, Node n INNER JOIN p.owner o LEFT JOIN n.refined n1 LEFT JOIN n1.refined n2"
			+ " LEFT JOIN FETCH p.depends WHERE n.id = :node AND (o.id=n.id OR o.id=n1.id OR o.id=n2.id)"
			+ " AND (p.mode = org.ligoj.app.api.SubscriptionMode.ALL OR p.mode = :mode) AND "
			+ NodeRepository.VISIBLE_NODES
			+ " AND NOT EXISTS (SELECT 1 FROM ParameterValue WHERE parameter = p AND (node.id=n.id OR node.id=n1.id OR node.id=n2.id))")
	List<Parameter> getOrphanParameters(String node, SubscriptionMode mode, String user);

	/**
	 * Return all parameters associated to a node but without specified value for the given node's parents. The
	 * parameters are filtered by the requested mode. Also check the node is visible for the given user. Parameter
	 * having a value associated directly to the given node will be returned. This is the sole difference with
	 * {@link #getOrphanParameters(String, SubscriptionMode, String)}
	 *
	 * @param node The parent node identifier.
	 * @param mode Expected mode.
	 * @param user The user requesting the nodes.
	 * @return all parameters associated to a node but without a value.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT p FROM Parameter p, Node n INNER JOIN p.owner o LEFT JOIN n.refined n1 LEFT JOIN n1.refined n2 WHERE n.id = :node AND (o.id=n.id OR o.id=n1.id OR o.id=n2.id)"
			+ " AND (p.mode = org.ligoj.app.api.SubscriptionMode.ALL OR p.mode = :mode) AND "
			+ NodeRepository.VISIBLE_NODES
			+ " AND NOT EXISTS (SELECT 1 FROM ParameterValue WHERE parameter = p AND node.id != :node AND (node.id=n.id OR node.id=n1.id OR node.id=n2.id)) ORDER BY UPPER(cast(p.id as String))")
	List<Parameter> getOrphanParametersExt(String node, SubscriptionMode mode, String user);

	/**
	 * Return the parameter with the given identifier and associated to a visible node by the given user.
	 *
	 * @param id   The parameter identifier.
	 * @param user The user principal requesting this parameter.
	 * @return The visible parameter or <code>null</code> when not found.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT p FROM Parameter p INNER JOIN p.owner n WHERE p.id=:id AND " + NodeRepository.VISIBLE_NODES)
	Parameter findOneVisible(String id, String user);

	/**
	 * Delete all parameters related to the given node or sub-nodes.
	 *
	 * @param node The node identifier.
	 */
	@SuppressWarnings("unused")
	@Modifying
	@Query("DELETE Parameter WHERE owner.id = :node OR owner.id LIKE CONCAT(:node, ':%')")
	void deleteByNode(String node);
}

package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.model.ParameterValue;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link ParameterValue} repository
 */
public interface ParameterValueRepository extends RestRepository<ParameterValue, Integer> {

	/**
	 * Return all parameter values associated to a node, including the ones from
	 * the parent.
	 * 
	 * @param node
	 *            The node identifier.
	 * @return All parameter values associated to a node.
	 */
	@Query("SELECT p FROM ParameterValue p LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE n0.id = :node OR n1.refined.id = :node OR n2.refined.id = :node")
	List<ParameterValue> getParameterValues(String node);

	/**
	 * Return a parameter value related to the subscription to the given service
	 * for a project.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 * @param parameter
	 *            The parameter identifier.
	 * @return the associated parameter value as {@link String}
	 */
	@Query("SELECT p.data FROM ParameterValue p, Subscription s INNER JOIN s.node service LEFT JOIN p.subscription subscription LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE s.id = ?1 AND (subscription = s OR  n0 = service OR n1.refined = service OR n2.refined = service) AND p.parameter.id = ?2")
	String getSubscriptionParameterValue(int subscription, String parameter);

	/**
	 * Return all parameters (name and raw value) associated to a subscription.
	 * Sensitive parameters are returned.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 * @return all parameters associated to a subscription.
	 */
	@Query("SELECT p FROM ParameterValue p, Subscription s INNER JOIN s.node service INNER JOIN FETCH p.parameter"
			+ " LEFT JOIN p.subscription subscription LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE s.id = ?1 AND (subscription = s OR  n0 = service OR n1.refined = service OR n2.refined = service)")
	List<ParameterValue> findAllBySubscription(int subscription);

	/**
	 * Return all non secured parameters (name and raw value) associated to a
	 * subscription. Sensitive parameters are not returned.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 * @return all parameters associated to a subscription.
	 */
	@Query("SELECT v FROM ParameterValue v, Subscription s INNER JOIN s.node service INNER JOIN FETCH v.parameter AS param"
			+ " LEFT JOIN v.subscription subscription LEFT JOIN v.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE s.id = ?1 AND param.secured != TRUE"
			+ " AND (subscription = s OR  n0 = service OR n1.refined = service OR n2.refined = service)")
	List<ParameterValue> findAllSecureBySubscription(int subscription);

	/**
	 * Delete all parameter values related to the given node or sub-nodes.
	 * 
	 * @param node
	 *            The node identifier.
	 */
	@Modifying
	@Query("DELETE ParameterValue WHERE"
			+ "    parameter.id IN (SELECT id FROM Parameter WHERE owner.id = :node OR owner.id LIKE CONCAT(:node, ':%'))"
			+ " OR subscription.id IN (SELECT id FROM Subscription WHERE node.id = :node OR node.id LIKE CONCAT(:node, ':%'))"
			+ " OR node.id = :node OR node.id LIKE CONCAT(:node, ':%') ")
	void deleteByNode(String node);

	/**
	 * Return the parameter with the given identifier and associated to a
	 * visible and also writable node by the given user. Only entities linked to
	 * a node can be deleted this way.
	 * 
	 * @param id
	 *            The parameter identifier.
	 * @param user
	 *            The user principal requesting this parameter.
	 * @return The visible parameter or <code>null</code> when not found.
	 */
	@Query("FROM ParameterValue v INNER JOIN FETCH v.node n WHERE v.id=:id AND n IS NOT NULL AND " + NodeRepository.WRITE_NODES)
	ParameterValue findOneVisible(int id, String user);
}

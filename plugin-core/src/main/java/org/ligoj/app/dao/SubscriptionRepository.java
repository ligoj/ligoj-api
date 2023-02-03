/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Subscription} repository
 */
@SuppressWarnings("ALL")
public interface SubscriptionRepository extends RestRepository<Subscription, Integer> {

	/**
	 * Return all subscriptions with only little information.
	 *
	 * @return the subscription's data :project identifier and node identifier.
	 */
	@Query("SELECT s.id, p.id, se.id FROM Subscription s INNER JOIN s.node AS se INNER JOIN s.project AS p")
	List<Object[]> findAllLight();

	/**
	 * Return the subscriptions of given project.
	 *
	 * @param project the subscribing project
	 * @return the subscriptions of given project.
	 */
	@Query("FROM Subscription s INNER JOIN FETCH s.node WHERE s.project.id = :project")
	List<Subscription> findAllByProject(int project);

	/**
	 * Return all subscriptions attached to the same project as the given subscription.
	 *
	 * @param subscription the subscription used to check the other attached a common project.
	 * @return the subscriptions attached to the same project. Service are fetch.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT s1 FROM Subscription s1, Subscription s2 INNER JOIN FETCH s1.node WHERE s2.id = :subscription AND s1.project.id = s2.project.id")
	List<Subscription> findAllOnSameProject(int subscription);

	/**
	 * Return the subscriptions to given node or one of the sub-nodes, and with all unsecured parameters.
	 *
	 * @param node the subscribed node. Directly or not.
	 * @return the subscriptions to given node.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT s, p FROM Subscription s, ParameterValue p INNER JOIN FETCH s.node service LEFT JOIN p.subscription subscription INNER JOIN FETCH p.parameter param "
			+ " LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2 LEFT JOIN service.refined sn0 LEFT JOIN sn0.refined sn1"
			+ " WHERE (service.id = :node OR sn0.id = :node OR sn1.id = :node)"
			+ "   AND (subscription = s OR  n0.id = service.id OR n1.refined.id = service.id OR n2.refined.id = service.id) AND param.secured != TRUE")
	List<Object[]> findAllWithValuesSecureByNode(String node);

	/**
	 * Return the amount of subscriptions to given node or one of the sub-nodes.
	 *
	 * @param node The subscribed node. Directly or not.
	 * @return The amount of subscriptions to given node.
	 */
	@Query("SELECT count(s.id) FROM Subscription s INNER JOIN s.node service "
			+ " LEFT JOIN service.refined sn0 LEFT JOIN sn0.refined sn1 WHERE (service.id = :node OR sn0.id = :node OR sn1.id = :node)")
	int countByNode(String node);

	/**
	 * Return the amount of subscriptions involving the given parameter value.
	 *
	 * @param parameterValue The parameter value identifier.
	 * @return The amount of subscriptions involving the given parameter value directly or not.
	 */
	@Query("SELECT count(s.id) FROM Subscription s, ParameterValue v INNER JOIN s.node sn INNER JOIN v.node vn"
			+ " WHERE v.id = :parameterValue AND (vn.id = sn.id OR sn.id LIKE CONCAT(vn.id, ':%'))")
	int countByParameterValue(int parameterValue);

	/**
	 * Return the subscriptions of given project with all unsecured parameters.
	 *
	 * @param project the subscribing project
	 * @return the subscriptions of given project.
	 */
	@Query("SELECT s, p FROM Subscription s, ParameterValue p INNER JOIN FETCH s.node service LEFT JOIN p.subscription subscription INNER JOIN FETCH p.parameter param "
			+ " LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE s.project.id = :project AND (subscription.id = s.id OR  n0.id = service.id OR n1.refined.id = service.id OR n2.refined.id = service.id) AND param.secured != TRUE")
	List<Object[]> findAllWithValuesSecureByProject(int project);

	/**
	 * Return all subscriptions and associated parameters on the given node.
	 *
	 * @param node node identifier.
	 * @return subscriptions (index=0) and associated parameters (index=1)
	 */
	@Query("SELECT s, p FROM ParameterValue p INNER JOIN p.subscription s INNER JOIN s.node service INNER JOIN FETCH p.parameter "
			+ " LEFT JOIN s.node n0 WHERE n0.id = :node")
	List<Object[]> findAllWithValuesByNode(String node);

	/**
	 * Count subscriptions by project's identifier.
	 *
	 * @param project project's identifier.
	 * @return Amount of subscriptions to the given project.
	 */
	@Query("SELECT COUNT(id) FROM Subscription WHERE project.id = :project")
	long countByProject(int project);
}

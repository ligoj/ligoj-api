package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.model.Subscription;

/**
 * {@link Subscription} repository
 */
public interface SubscriptionRepository extends RestRepository<Subscription, Integer> {

	/**
	 * Return all visible subscriptions visible for current user.
	 * 
	 * @param user
	 *            the current user.
	 * @return the subscriptions data (project identifier and node identifier) visible for current user..
	 */
	@Query("SELECT s.id, p.id, se.id FROM Subscription s INNER JOIN s.node AS se INNER JOIN s.project AS p WHERE "
			+ ProjectRepository.VISIBLE_PROJECTS)
	List<Object[]> findAllLight(String user);

	/**
	 * Return the subscriptions of given project.
	 * 
	 * @param project
	 *            the subscribing project
	 * @return the subscriptions of given project.
	 */
	@Query("FROM Subscription s INNER JOIN FETCH s.node WHERE s.project.id = ?1")
	List<Subscription> findAllByProject(int project);

	/**
	 * Return all subscriptions attached to the same project than the given subscription.
	 * 
	 * @param subscription
	 *            the subscription used to check the other attached a common project.
	 * @return the subscriptions attached to the same project. Service are fetch.
	 */
	@Query("SELECT s1 FROM Subscription s1, Subscription s2 INNER JOIN FETCH s1.node WHERE s2.id = ?1 AND s1.project.id = s2.project.id")
	List<Subscription> findAllOnSameProject(int subscription);

	/**
	 * Return the subscriptions to given node or one of the sub-nodes, and with all non secured parameters.
	 * 
	 * @param node
	 *            the subscribed node. Directly or not.
	 * @return the subscriptions to given node.
	 */
	@Query("SELECT s, p FROM Subscription s, ParameterValue p INNER JOIN FETCH s.node service LEFT JOIN p.subscription subscription INNER JOIN FETCH p.parameter param "
			+ " LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2 LEFT JOIN service.refined sn0 LEFT JOIN sn0.refined sn1"
			+ " WHERE (service.id = ?1 OR sn0.id = ?1 OR sn1.id = ?1)"
			+ "   AND (subscription = s OR  n0 = service OR n1.refined = service OR n2.refined = service) AND param.secured != TRUE")
	List<Object[]> findAllWithValuesSecureByNode(String node);

	/**
	 * Return the amount of subscriptions to given node or one of the sub-nodes.
	 * 
	 * @param node
	 *            The subscribed node. Directly or not.
	 * @return The amount of subscriptions to given node.
	 */
	@Query("SELECT count(s.id) FROM Subscription s INNER JOIN s.node service "
			+ " LEFT JOIN service.refined sn0 LEFT JOIN sn0.refined sn1 WHERE (service.id = ?1 OR sn0.id = ?1 OR sn1.id = ?1)")
	int countByNode(String node);

	/**
	 * Return the subscriptions of given project with all non secured parameters.
	 * 
	 * @param project
	 *            the subscribing project
	 * @return the subscriptions of given project.
	 */
	@Query("SELECT s, p FROM Subscription s, ParameterValue p INNER JOIN FETCH s.node service LEFT JOIN p.subscription subscription INNER JOIN FETCH p.parameter param "
			+ " LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE s.project.id = ?1 AND (subscription = s OR  n0 = service OR n1.refined = service OR n2.refined = service) AND param.secured != TRUE"
			+ " ORDER BY service.id")
	List<Object[]> findAllWithValuesSecureByProject(int project);

	/**
	 * Return all subscriptions and associated parameters on the given node.
	 * 
	 * @param node
	 *            node identifier.
	 * @return subscriptions (index=0) and associated parameters (index=1)
	 */
	@Query("SELECT s, p FROM ParameterValue p INNER JOIN p.subscription s INNER JOIN s.node service INNER JOIN FETCH p.parameter "
			+ " LEFT JOIN s.node n0 WHERE n0.id = ?1  ")
	List<Object[]> findAllWithValuesByNode(String node);

	/**
	 * Count subscriptions by project's identifier.
	 * 
	 * @param project
	 *            project's identifier.
	 * @return Amount of subscriptions to the given project.
	 */
	@Query("SELECT COUNT(id) FROM Subscription WHERE project.id = :project")
	long countByProject(int project);

}

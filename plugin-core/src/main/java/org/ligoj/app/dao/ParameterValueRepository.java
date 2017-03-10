package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.model.ParameterValue;

/**
 * {@link ParameterValue} repository
 */
public interface ParameterValueRepository extends RestRepository<ParameterValue, Integer> {

	/**
	 * Return a parameter value related to the subscription to the given service for a project.
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
	 * Return all parameters (name and raw value) associated to a subscription. Sensitive parameters are returned.
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
	 * Return all non secured parameters (name and raw value) associated to a subscription. Sensitive parameters are not
	 * returned.
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
}

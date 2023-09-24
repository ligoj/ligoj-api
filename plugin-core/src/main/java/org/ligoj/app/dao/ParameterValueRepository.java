/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.model.ParameterValue;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link ParameterValue} repository
 */
@SuppressWarnings("ALL")
public interface ParameterValueRepository extends RestRepository<ParameterValue, Integer> {

	String RELATED_SUBSCRIPTION = "  (s.id = :subscription AND (v.subscription.id = :subscription OR v.node.id = s.node.id OR s.node.id LIKE CONCAT(v.node.id, ':%')))";

	/**
	 * Return all parameter values associated to a node, including the ones from the parent.
	 *
	 * @param node The node identifier.
	 * @return All parameter values associated to a node.
	 */
	@SuppressWarnings("unused")
	@Query("FROM ParameterValue v INNER JOIN v.node n WHERE n.id = :node OR :node LIKE CONCAT(n.id, ':%')")
	List<ParameterValue> getParameterValues(String node);

	/**
	 * Return a parameter raw value (secured or not) related to the subscription to the given service for a project.
	 *
	 * @param subscription the subscription identifier.
	 * @param parameter    The parameter identifier.
	 * @return the associated parameter raw value as {@link String}
	 */
	@SuppressWarnings("unused")
	@Query("""
			SELECT v.data FROM ParameterValue v, Subscription s
			    WHERE v.parameter.id = :parameter
			    AND
			""" + RELATED_SUBSCRIPTION)
	String getSubscriptionParameterValue(int subscription, String parameter);

	/**
	 * Return all parameters (name and raw value) associated to a subscription. Sensitive parameters are returned.
	 *
	 * @param subscription the subscription identifier.
	 * @return all parameters associated to a subscription.
	 */
	@SuppressWarnings("unused")
	@Query("""
			SELECT v FROM ParameterValue v, Subscription s
			    WHERE
			""" + RELATED_SUBSCRIPTION)
	List<ParameterValue> findAllBySubscription(int subscription);
	//  (s.id = :subscription AND (v.subscription.id = :subscription OR v.node.id = s.node.id OR s.node.id LIKE CONCAT(v.node.id, ':%')))

	/**
	 * Return all unsecured parameters (name and raw value) associated to a subscription. Sensitive parameters are not
	 * returned.
	 *
	 * @param subscription the subscription identifier.
	 * @return all parameters associated to a subscription.
	 */
	@SuppressWarnings("unused")
	@Query("""
			SELECT v FROM ParameterValue v, Subscription s
			    INNER JOIN FETCH v.parameter AS param
				WHERE param.secured != TRUE AND
			""" + RELATED_SUBSCRIPTION)
	List<ParameterValue> findAllSecureBySubscription(int subscription);

	/**
	 * Delete all parameter values related to the given node or sub-nodes.
	 *
	 * @param node The node identifier.
	 */
	@SuppressWarnings("unused")
	@Modifying
	@Query("""
			DELETE ParameterValue WHERE
			       parameter.id IN (SELECT id FROM Parameter WHERE owner.id = :node OR owner.id LIKE CONCAT(:node, ':%'))
				OR subscription.id IN (SELECT id FROM Subscription WHERE node.id = :node OR node.id LIKE CONCAT(:node, ':%'))
				OR node.id = :node
				OR node.id LIKE CONCAT(:node, ':%')
			""")
	void deleteByNode(String node);

	/**
	 * Return the parameter with the given identifier and associated to a visible and also writable node by the given
	 * user. Only entities linked to a node can be deleted this way.
	 *
	 * @param id   The parameter identifier.
	 * @param user The user principal requesting this parameter.
	 * @return The visible parameter or <code>null</code> when not found.
	 */
	@SuppressWarnings("unused")
	@Query("FROM ParameterValue v INNER JOIN FETCH v.node n WHERE v.id=:id AND n IS NOT NULL AND "
			+ NodeRepository.WRITE_NODES)
	ParameterValue findOneVisible(int id, String user);

	/**
	 * Return the {@link ParameterValue} related to given parameter name and associated ot given project and given node.
	 *
	 * @param node      The subscribed node. Directly or not.
	 * @param parameter The id of the parameter.
	 * @param project   project's identifier.
	 * @param criteria  the optional criteria used to check name (CN).
	 * @return A list of table of [Subscription, ParameterValue]
	 */
	@SuppressWarnings("unused")
	@Query("""
			SELECT v FROM ParameterValue v, Subscription s
			    INNER JOIN s.node n
			    INNER JOIN FETCH v.parameter AS param
				WHERE s.project.id = :project
					AND param.id = :parameter
					AND UPPER(v.data) LIKE UPPER(CONCAT(CONCAT('%', :criteria),'%'))
					AND param.secured != TRUE
					AND (s.node.id = :node OR s.node.id LIKE CONCAT(:node, ':%'))
					AND (v.subscription.id = s.id OR s.node.id LIKE CONCAT(v.node.id, ':%'))
				ORDER BY v.data, v.id
			""")
	List<ParameterValue> findAll(String node, String parameter, int project, String criteria);

}

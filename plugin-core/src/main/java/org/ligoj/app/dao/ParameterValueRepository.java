/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.ArrayList;
import java.util.Collection;
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

	/**
	 * Values of the subscription itself, or of the subscribed node and its ancestors: <code>:nodes</code> is the
	 * result of {@link #toAncestors(String)} for the subscribed node. The ancestors are matched by identifier so the
	 * node index is used, instead of a <code>LIKE</code> pattern built from each value's node.
	 */
	String RELATED_SUBSCRIPTION = "  (v.subscription.id = :subscription OR v.node.id IN :nodes)";

	/**
	 * Return the given node identifier followed by the identifiers of its ancestors, from the closest to the root.
	 * Node identifiers are hierarchical: <code>service:bt:jira:6</code> is under <code>service:bt:jira</code>, under
	 * <code>service:bt</code>, under <code>service</code>.
	 *
	 * @param node The node identifier. May be <code>null</code>.
	 * @return The node and its ancestor identifiers. Empty when the node is <code>null</code>.
	 */
	static List<String> toAncestors(final String node) {
		final var result = new ArrayList<String>();
		if (node != null) {
			result.add(node);
			for (var index = node.lastIndexOf(':'); index > 0; index = node.lastIndexOf(':', index - 1)) {
				result.add(node.substring(0, index));
			}
		}
		return result;
	}

	/**
	 * Return all parameter values associated to a node, including the ones from the parent.
	 *
	 * @param node The node identifier.
	 * @return All parameter values associated to a node.
	 */
	default List<ParameterValue> getParameterValues(final String node) {
		if (node == null) {
			return new ArrayList<>();
		}
		return findAllByNodes(toAncestors(node));
	}

	/**
	 * Return all parameter values attached to one of the given nodes.
	 *
	 * @param nodes The node identifiers.
	 * @return All parameter values attached to one of the given nodes, ordered by identifier.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT v FROM ParameterValue v WHERE v.node.id IN :nodes ORDER BY v.id")
	List<ParameterValue> findAllByNodes(Collection<String> nodes);

	/**
	 * Return the node identifier of a subscription.
	 *
	 * @param subscription the subscription identifier.
	 * @return The subscribed node identifier or <code>null</code> when the subscription does not exist.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT s.node.id FROM Subscription s WHERE s.id = :subscription")
	String findNodeBySubscription(int subscription);

	/**
	 * Return a parameter raw value (secured or not) of a subscription: the value of the subscription itself, or the
	 * value inherited from the subscribed node or one of its parents.
	 *
	 * @param subscription the subscription identifier.
	 * @param parameter    The parameter identifier.
	 * @return the associated parameter raw value as {@link String}, <code>null</code> when the subscription does not
	 *         exist or when there is no value.
	 */
	default String getSubscriptionParameterValue(final int subscription, final String parameter) {
		final var node = findNodeBySubscription(subscription);
		return node == null ? null : getSubscriptionParameterValue(subscription, parameter, toAncestors(node));
	}

	/**
	 * Return a parameter raw value (secured or not) related to the subscription or to the given nodes.
	 *
	 * @param subscription the subscription identifier.
	 * @param parameter    The parameter identifier.
	 * @param nodes        The subscribed node and its ancestors.
	 * @return the associated parameter raw value as {@link String}
	 */
	@SuppressWarnings("unused")
	@Query("SELECT v.data FROM ParameterValue v WHERE v.parameter.id = :parameter AND " + RELATED_SUBSCRIPTION)
	String getSubscriptionParameterValue(int subscription, String parameter, Collection<String> nodes);

	/**
	 * Return all parameters (name and raw value) associated to a subscription, including the values inherited from the
	 * subscribed node and its parents. Sensitive parameters are returned.
	 *
	 * @param subscription the subscription identifier.
	 * @return all parameters associated to a subscription. Empty when the subscription does not exist.
	 */
	default List<ParameterValue> findAllBySubscription(final int subscription) {
		final var node = findNodeBySubscription(subscription);
		return node == null ? new ArrayList<>() : findAllBySubscription(subscription, toAncestors(node));
	}

	/**
	 * Return all parameters (name and raw value) associated to a subscription or to the given nodes. Sensitive
	 * parameters are returned.
	 *
	 * @param subscription the subscription identifier.
	 * @param nodes        The subscribed node and its ancestors.
	 * @return all parameters associated to a subscription.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT v FROM ParameterValue v WHERE " + RELATED_SUBSCRIPTION)
	List<ParameterValue> findAllBySubscription(int subscription, Collection<String> nodes);

	/**
	 * Return all unsecured parameters (name and raw value) associated to a subscription, including the values
	 * inherited from the subscribed node and its parents. Sensitive parameters are not returned.
	 *
	 * @param subscription the subscription identifier.
	 * @return all parameters associated to a subscription. Empty when the subscription does not exist.
	 */
	default List<ParameterValue> findAllSecureBySubscription(final int subscription) {
		final var node = findNodeBySubscription(subscription);
		return node == null ? new ArrayList<>() : findAllSecureBySubscription(subscription, toAncestors(node));
	}

	/**
	 * Return all unsecured parameters (name and raw value) associated to a subscription or to the given nodes.
	 * Sensitive parameters are not returned.
	 *
	 * @param subscription the subscription identifier.
	 * @param nodes        The subscribed node and its ancestors.
	 * @return all parameters associated to a subscription.
	 */
	@SuppressWarnings("unused")
	@Query("""
			SELECT v FROM ParameterValue v
			    INNER JOIN FETCH v.parameter AS param
			    WHERE param.secured != TRUE AND
			""" + RELATED_SUBSCRIPTION)
	List<ParameterValue> findAllSecureBySubscription(int subscription, Collection<String> nodes);

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
	@Query("SELECT v FROM ParameterValue v INNER JOIN FETCH v.node n WHERE v.id=:id AND n IS NOT NULL AND "
			+ NodeRepository.WRITE_NODES)
	ParameterValue findOneVisible(int id, String user);

	/**
	 * Return the non-secured values of a parameter, used by the subscriptions of a project to a node or one of its
	 * sub-nodes: the values of the subscriptions themselves, and the values inherited from a parent node of the
	 * subscribed node.
	 *
	 * @param node      The subscribed node. Directly or not.
	 * @param parameter The id of the parameter.
	 * @param project   project's identifier.
	 * @param criteria  the optional criteria used to check name (CN).
	 * @return The matching parameter values, ordered by data then identifier.
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

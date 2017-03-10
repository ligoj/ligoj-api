package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.dao.ldap.DelegateLdapRepository;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;

/**
 * {@link Node} repository
 */
public interface NodeRepository extends RestRepository<Node, String> {

	String MATCH_DELEGATE = DelegateLdapRepository.ASSIGNED_DELEGATE + " AND (n.id LIKE CONCAT(d.name, ':%') OR d.name=n.id)";

	/**
	 * Visible nodes condition.
	 */
	String VISIBLE_NODES_PART = " EXISTS(SELECT 1 FROM DelegateNode d WHERE " + MATCH_DELEGATE;

	/**
	 * Visible nodes condition.
	 */
	String VISIBLE_NODES = VISIBLE_NODES_PART + ")";

	/**
	 * Visible nodes condition to subscribe projects : either administrator, either can edit it, either administer it.
	 */
	String SUBSCRIBE_NODES = VISIBLE_NODES_PART + " AND (d.canSubscribe = true OR d.canWrite = true OR d.canAdmin = true))";

	/**
	 * Return all parameter values associated to a node, or a refined one.
	 * 
	 * @param id
	 *            the node identifier.
	 * @return all parameter values associated to a node.
	 */
	@Query("SELECT p FROM ParameterValue p LEFT JOIN p.node n0 LEFT JOIN n0.refined n1 LEFT JOIN n1.refined n2"
			+ " WHERE n0.id = :id OR n1.refined.id = :id OR n2.refined.id = :id")
	List<ParameterValue> getParameterValues(String id);

	/**
	 * Return all parameters associated to a node and without specified value and for given mode. Also check the node is
	 * visible for the given user.
	 * 
	 * @param id
	 *            the node identifier.
	 * @param mode
	 *            Expected mode.
	 * @param user
	 *            The user requesting the nodes.
	 * @return all parameters associated to a node.
	 */
	@Query("SELECT p FROM Parameter p, Node n INNER JOIN p.owner o LEFT JOIN n.refined n1 LEFT JOIN n1.refined n2 WHERE n.id = :id AND (o=n OR o=n1 OR o=n2)"
			+ " AND (p.mode = NULL OR p.mode = :mode) AND " + VISIBLE_NODES
			+ " AND NOT EXISTS (SELECT 1 FROM ParameterValue v WHERE v.parameter = p AND (v.node=n OR v.node=n1 OR v.node=n2)) ORDER BY UPPER(p.name)")
	List<Parameter> getOrphanParameters(String id, SubscriptionMode mode, String user);

	/**
	 * Return all nodes with all non secured parameters.
	 * 
	 * @return the nodes.
	 */
	@Query("SELECT n, p FROM ParameterValue p RIGHT JOIN p.node n"
			+ " LEFT JOIN p.parameter param ON (param.id=p.parameter.id AND param.secured != TRUE) ORDER BY UPPER(n.name)")
	List<Object[]> findAllWithValuesSecure();

	/**
	 * Return nodes with given refined node
	 * 
	 * @param parent
	 *            Optional parent node identifier.
	 * @param mode
	 *            Expected mode. When <code>null</code>, the node's mode is not checked.
	 * @param user
	 *            The user requesting the nodes.
	 * @return Nodes with given refined node
	 */
	@Query("FROM Node n WHERE (n.refined.id = :parent OR (:parent IS NULL AND n.refined IS NULL))                                             "
			+ " AND (:mode IS NULL OR (n.mode = :mode OR n.mode = org.ligoj.app.api.SubscriptionMode.CREATE))                                 "
			+ " AND " + VISIBLE_NODES + " ORDER BY UPPER(n.name)")
	List<Node> findAllByParent(String parent, SubscriptionMode mode, String user);

	/**
	 * Return final nodes : associated to a tool
	 * 
	 * @return instance nodes considered as final .
	 */
	@Query("FROM Node n INNER JOIN FETCH n.refined tool WHERE tool.refined IS NOT NULL ORDER BY UPPER(n.name)")
	List<Node> findAllInstance();

	/**
	 * Return final nodes associated to a tool and visible for a given user.
	 * 
	 * @param user
	 *            The user requesting the nodes.
	 * @return instance nodes considered as final .
	 */
	@Query("FROM Node n INNER JOIN FETCH n.refined tool WHERE tool.refined IS NOT NULL AND " + VISIBLE_NODES + " ORDER BY UPPER(n.name)")
	List<Node> findAllInstance(String user);

	/**
	 * Count subscriptions by node and visible to a given user.
	 * 
	 * @param user
	 *            The user requesting the nodes.
	 * @return node subscriptions count
	 */
	@Query("SELECT n.id, count(sub) FROM Subscription sub INNER JOIN sub.node n WHERE " + VISIBLE_NODES + " GROUP BY n.id")
	List<Object[]> countNodeSubscriptions(String user);

	/**
	 * Return a {@link Node} by its identifier if it is visible for current user.
	 * 
	 * @param id
	 *            The identifier to find.
	 * @param user
	 *            The user requesting the node.
	 * @return The visible node or <code>null</code>.
	 */
	@Query("FROM Node n WHERE n.id=:id AND " + VISIBLE_NODES)
	Node findOneVisible(String id, String user);

	/**
	 * Return all visible {@link Node} for current user.
	 * 
	 * @param user
	 *            The user requesting the nodes.
	 * @param criteria
	 *            the optional criteria to match.
	 * @param page
	 *            the pagination.
	 * @return The visible nodes.
	 */
	@Query("FROM Node n WHERE (:criteria IS NULL OR UPPER(n.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) AND " + VISIBLE_NODES)
	Page<Node> findAllVisible(String user, String criteria, Pageable page);

	/**
	 * Return a {@link Node} by its identifier if it is visible for current user and if this user can create a
	 * subscription on it.
	 * 
	 * @param id
	 *            The identifier to find.
	 * @param user
	 *            The user requesting the node.
	 * @return The visible node the user can subscribe or <code>null</code>.
	 */
	@Query("FROM Node n WHERE n.id=:id AND " + SUBSCRIBE_NODES)
	Node findOneForSubscription(String id, String user);

	/**
	 * Return the amount of nodes having the parent node.
	 * 
	 * @param node
	 *            The parent node identifier. Directly or not.
	 * @return The amount of nodes having the parent node.
	 */
	@Query("SELECT count(id) FROM Node WHERE refined.refined.id = ?1 OR refined.id = ?1")
	int countByRefined(String node);

}

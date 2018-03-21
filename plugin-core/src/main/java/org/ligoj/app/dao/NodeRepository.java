/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.model.Node;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.dao.system.SystemUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Node} repository
 */
public interface NodeRepository extends RestRepository<Node, String> {

	String MATCH_DELEGATE = "(n.id LIKE CONCAT(d.name, ':%') OR d.name=n.id) AND " + DelegateOrgRepository.ASSIGNED_DELEGATE;

	/**
	 * Visible nodes condition.
	 */
	String VISIBLE_NODES_PART = "(" + SystemUserRepository.IS_ADMIN + " OR EXISTS(SELECT 1 FROM DelegateNode d WHERE " + MATCH_DELEGATE;

	/**
	 * Visible nodes condition.
	 */
	String VISIBLE_NODES = VISIBLE_NODES_PART + "))";

	/**
	 * Visible nodes condition to subscribe projects : either administrator, either
	 * can edit it, either administer it.
	 */
	String SUBSCRIBE_NODES = VISIBLE_NODES_PART + " AND (d.canSubscribe = true OR d.canWrite = true OR d.canAdmin = true)))";

	/**
	 * Visible nodes condition for update : either administrator, either can edit
	 * it, either administer it.
	 */
	String WRITE_NODES = VISIBLE_NODES_PART + " AND (d.canWrite = true OR d.canAdmin = true)))";

	/**
	 * Visible nodes condition for deletion : either administrator, either
	 * administer it.
	 */
	String ADMIN_NODES = VISIBLE_NODES_PART + " AND d.canAdmin = true))";

	/**
	 * Return all nodes with all non secured parameters.
	 * 
	 * @return the nodes.
	 */
	@Query("SELECT n, p FROM ParameterValue p RIGHT JOIN p.node n"
			+ " LEFT JOIN p.parameter param ON (param.id=p.parameter.id AND param.secured != TRUE) ORDER BY UPPER(n.name)")
	List<Object[]> findAllWithValuesSecure();

	/**
	 * Return final nodes, so representing a node (running instance) of a tool.
	 * 
	 * @return instance nodes considered as final .
	 */
	@Query("FROM Node n INNER JOIN FETCH n.refined tool WHERE tool.refined IS NOT NULL ORDER BY UPPER(n.name)")
	List<Node> findAllInstance();

	/**
	 * Return final nodes, so representing a node (running instance) of a tool and
	 * visible for a given user.
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
	 * Return a {@link Node} by its identifier if it is visible for the current
	 * user.
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
	 * Return a {@link Node} by its identifier if it is visible and writable for the
	 * current user.
	 * 
	 * @param id
	 *            The identifier to find.
	 * @param user
	 *            The user requesting the node.
	 * @return The visible node or <code>null</code>.
	 */
	@Query("FROM Node n WHERE n.id=:id AND " + WRITE_NODES)
	Node findOneWritable(String id, String user);

	/**
	 * Return a {@link Node} by its identifier if it is visible and administered for
	 * the current user.
	 * 
	 * @param id
	 *            The identifier to find.
	 * @param user
	 *            The user requesting the node.
	 * @return The visible node or <code>null</code>.
	 */
	@Query("FROM Node n WHERE n.id=:id AND " + ADMIN_NODES)
	Node findOneAdministerable(String id, String user);

	/**
	 * Return all visible {@link Node} for current user.
	 * 
	 * @param user
	 *            The user requesting the nodes.
	 * @param criteria
	 *            The optional criteria to match in the name.
	 * @param parent
	 *            The optional parent identifier to be like. Special attention for
	 *            'service' value corresponding to the root.
	 * @param mode
	 *            Expected subscription mode. When <code>null</code>, the node's
	 *            mode is not checked.
	 * @param depth
	 *            The maximal depth. When <code>0</code> means no refined, so
	 *            basically services only. <code>1</code> means refined is a
	 *            service, so nodes are basically tool only. <code>2</code> means
	 *            refined is a tool, so nodes are basically instances only. For the
	 *            other cases, there is no limit.
	 * @param page
	 *            The pagination.
	 * @return The visible nodes. Ordered by their identifier.
	 */
	@Query("SELECT n FROM Node n LEFT JOIN n.refined nr1 LEFT JOIN nr1.refined nr2"
			+ " WHERE (:parent IS NULL OR (:parent = 'service' AND n.refined IS NULL) OR n.refined.id = :parent)"
			+ " AND (:depth < 0 OR :depth > 1 OR (:depth = 0 AND nr1 IS NULL) OR (:depth = 1 AND nr2 IS NULL))  "
			+ " AND (:mode IS NULL OR n.mode = :mode OR n.mode = org.ligoj.app.api.SubscriptionMode.ALL)        "
			+ " AND (UPPER(n.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) AND " + VISIBLE_NODES + " ORDER BY n.id")
	Page<Node> findAllVisible(String user, String criteria, String parent, SubscriptionMode mode, int depth, Pageable page);

	/**
	 * Return a {@link Node} by its identifier if it is visible for current user and
	 * if this user can create a subscription on it.
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

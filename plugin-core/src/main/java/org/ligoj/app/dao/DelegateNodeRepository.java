/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link DelegateNode} repository
 */
@SuppressWarnings("ALL")
public interface DelegateNodeRepository extends RestRepository<DelegateNode, Integer> {

	/**
	 * A visible {@link DelegateNode} with possible extension for constraint.
	 */
	String VISIBLE_DELEGATE_PART = "SELECT id FROM DelegateNode dz WHERE (d.name LIKE CONCAT(dz.name, ':%') OR d.name  = dz.name) "
			+ " AND " + DelegateOrgRepository.ASSIGNED_DELEGATE_DZ;

	/**
	 * A visible {@link DelegateNode}
	 */
	String VISIBLE_DELEGATE = "EXISTS (" + VISIBLE_DELEGATE_PART + ")";

	/**
	 * Return all {@link DelegateNode} objects regarding the given criteria.
	 *
	 * @param user     The user requesting the objects.
	 * @param criteria Optional, use to match by LDAP object name or target user.
	 * @param page     the pagination.
	 * @return all {@link DelegateNode} objects with the given name. Insensitive case search is used.
	 */
	@SuppressWarnings("unused")
	@Query("""
			SELECT d FROM DelegateNode d WHERE (:criteria = ''                                                           "
			       OR (UPPER(d.receiver) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))
			          OR UPPER(d.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%')))) AND 
			"""
			+ VISIBLE_DELEGATE)
	Page<DelegateNode> findAll(String user, String criteria, Pageable page);

	/**
	 * Return a positive number if the given node can be updated or created by the given user. A node can be managed
	 * when it is visible, and it exists at least one delegation with administration right for this node or one its
	 * parent.
	 *
	 * @param user  The user name requesting to manage a node.
	 * @param node  The related node to manage.
	 * @param write The <code>write</code> flag of the new delegate.
	 * @return A positive number if the given node can be managed by the given user.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT COUNT(d.id) FROM DelegateNode d WHERE d.canAdmin = true AND (d.canWrite = true OR :write = false)"
			+ " AND (:node LIKE CONCAT(d.name, ':%') OR d.name  = :node) AND " + DelegateOrgRepository.ASSIGNED_DELEGATE_D)
	int manageNode(String user, String node, boolean write);

	/**
	 * Return a visible DelegateNode, if it exists at least one delegation with administration right for this node or one its parent.
	 *
	 * @param id   The identifier of object to delete.
	 * @param user The user name requesting to manage a node.
	 * @return A positive number if the given delegate has been deleted.
	 */
	@Query("SELECT d FROM DelegateNode d WHERE d.id = :id AND EXISTS (" + VISIBLE_DELEGATE_PART + " AND dz.receiver=:user)")
	DelegateNode findById(int id, String user);
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.List;

import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Project;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Project} repository
 */
public interface ProjectRepository extends RestRepository<Project, Integer> {

	/**
	 * Visible projects condition where the principal user is either team leader, either member of one of the groups of
	 * this project.
	 */
	String MY_PROJECTS = "inproject(p,:user,:user)=true";

	/**
	 * Visible projects condition, using ID subscription and team leader attribute.
	 */
	String VISIBLE_PROJECTS = "(" + SystemUser.IS_ADMIN
			+ " OR visibleproject(p, cg.description, :user, :user, :user, :user, :user) = true)";

	/**
	 * Return all {@link Project} objects with visible by <code>user</code> and also filtered by a criteria. The
	 * constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param user
	 *            The principal user name
	 * @param criteria
	 *            the optional criteria to match: name, description or pkey. Case is insensitive.
	 * @param page
	 *            the pagination.
	 * @return all {@link Project} objects with the given name. Insensitive case search is used.
	 */
	@Query(value = "SELECT p, COUNT(DISTINCT s.id) FROM Project AS p LEFT JOIN p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg"
			+ " WHERE " + VISIBLE_PROJECTS + " AND (UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "       OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "       OR UPPER(p.pkey)        LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) GROUP BY p                ", countQuery = "SELECT COUNT(DISTINCT p) FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg"
					+ " WHERE " + VISIBLE_PROJECTS + " AND (UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
					+ "       OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
					+ "       OR UPPER(p.pkey)        LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) GROUP BY p")
	Page<Object[]> findAllLight(String user, String criteria, Pageable page);

	/**
	 * Return all {@link Project} objects having at least one subscription and with light information. The visibility is
	 * checked:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param user
	 *            The principal user name
	 * @return all visible {@link Project} objects for <code>user</code>.
	 */
	@Query("SELECT DISTINCT p.id, p.name, p.pkey FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE "
			+ VISIBLE_PROJECTS + " AND EXISTS(SELECT 1 FROM Subscription AS s WHERE s.project=p)")
	List<Object[]> findAllHavingSubscription(String user);

	/**
	 * Return a project by its identifier. The constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param id
	 *            The project's identifier to match.
	 * @param user
	 *            The current user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT DISTINCT p FROM Project AS p LEFT JOIN FETCH p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.id = :id AND "
			+ VISIBLE_PROJECTS)
	Project findOneVisible(int id, String user);

	/**
	 * Return a project by its <code>pkey</code>. The constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param pkey
	 *            The project <code>pkey</code> to match.
	 * @param user
	 *            The principal user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT DISTINCT p FROM Project AS p LEFT JOIN FETCH p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.pkey = :pkey AND "
			+ VISIBLE_PROJECTS)
	Project findByPKey(String pkey, String user);

	/**
	 * Return a project by its <code>pkey</code> without fetching the related subscriptions. The constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param pkey
	 *            The pkey to match.
	 * @param user
	 *            The principal user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT DISTINCT p FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.pkey = :pkey AND "
			+ VISIBLE_PROJECTS)
	Project findByPKeyNoFetch(String pkey, String user);

	/**
	 * Indicate <code>user</code> can manage the subscriptions of <code>project</code>. The constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> has at least one {@link org.ligoj.app.model.DelegateNode} with
	 * <code>canSubscribe</code>, and:</li>
	 * <ul>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either the project is visible by <code>user</code> and also <code>user</code> has at least one
	 * {@link org.ligoj.app.iam.model.DelegateOrg} with <code>canWrite</code> and related to the group associated to
	 * <code>project</code></li>
	 * </ul>
	 * </ul>
	 * Note, this will only authorize the principal to create subscriptions to this project, and the valid subscribed
	 * {@link Node}s should be filtered regarding the delegates on this node.
	 *
	 * @param project
	 *            The project's identifier to match.
	 * @param user
	 *            The principal user name.
	 * @return <code>true</code> when <code>user</code> can manage the subscriptions of this project.
	 */
	@Query("SELECT COUNT(p.id) > 0 FROM Project AS p LEFT JOIN p.cacheGroups AS cpg0 LEFT JOIN cpg0.group AS cg0 WHERE p.id = :project AND ("
			+ SystemUser.IS_ADMIN + "                          "
			+ "  OR (EXISTS(SELECT 1 FROM DelegateNode WHERE " + DelegateOrgRepository.ASSIGNED_DELEGATE
			+ "   AND canSubscribe = true)                                      "
			+ "  AND (p.teamLeader = :user                                      "
			+ "   OR (EXISTS(SELECT 1 FROM DelegateOrg WHERE " + DelegateOrgRepository.ASSIGNED_DELEGATE
			+ "    AND canWrite=true                                      "
			+ "    AND ((type=org.ligoj.app.iam.model.DelegateType.GROUP AND name=cg0.id) OR"
			+ "      (type=org.ligoj.app.iam.model.DelegateType.TREE"
			+ "       AND (cg0.description LIKE CONCAT('%,',dn) OR dn=cg0.description))))))))")
	boolean isManageSubscription(int project, String user);
}

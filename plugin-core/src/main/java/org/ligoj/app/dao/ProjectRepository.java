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
	String MY_PROJECTS = "inproject(:user,p.teamLeader)=true";

	/**
	 * Visible projects condition, using ID subscription and team leader attribute.
	 */
	String VISIBLE_PROJECTS = "(" + SystemUser.IS_ADMIN
			+ " OR visibleProject(p.teamLeader, cg.description, :user) = true)";

	/**
	 * Same as {@link #VISIBLE_PROJECTS}, without joining the groups of the project: the visibility is evaluated once
	 * per project instead of once per joined group row. A project without group is visible by its team leader.
	 */
	String VISIBLE_PROJECTS_EXISTS = "(" + SystemUser.IS_ADMIN + " OR p.teamLeader = :user"
			+ " OR EXISTS(SELECT 1 FROM CacheProjectGroup AS cpg INNER JOIN cpg.group AS cg WHERE cpg.project = p"
			+ "   AND visibleProject(p.teamLeader, cg.description, :user) = true))";

	/**
	 * Criteria matching the project name, description or key. Case is insensitive.
	 */
	String MATCH_CRITERIA = "(UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ " OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ " OR UPPER(p.pkey) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%')))";

	/**
	 * Return all {@link Project} objects with visible by <code>user</code> and also filtered by a criteria. The
	 * constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param user     The principal username
	 * @param criteria the optional criteria to match: name, description or pkey. Case is insensitive.
	 * @param page     the pagination.
	 * @return all {@link Project} objects with the given name. Insensitive case search is used.
	 */
	@Query(value = "SELECT p, COUNT(s.id) FROM Project AS p LEFT JOIN p.subscriptions AS s"
			+ " WHERE " + VISIBLE_PROJECTS_EXISTS + " AND " + MATCH_CRITERIA + " GROUP BY p",
			countQuery = "SELECT COUNT(p) FROM Project AS p WHERE " + VISIBLE_PROJECTS_EXISTS + " AND " + MATCH_CRITERIA)
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
	 * @param user The principal username
	 * @return all visible {@link Project} objects for <code>user</code>.
	 */
	@Query("SELECT p.id, p.name, p.pkey FROM Project AS p WHERE " + VISIBLE_PROJECTS_EXISTS
			+ " AND EXISTS(SELECT 1 FROM Subscription AS s WHERE s.project.id=p.id)")
	List<Object[]> findAllHavingSubscription(String user);

	/**
	 * Return a project by its identifier. The constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param id   The project's identifier to match.
	 * @param user The current username.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT DISTINCT p FROM Project AS p LEFT JOIN FETCH p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.id = :id AND "
			+ VISIBLE_PROJECTS)
	Project findOneVisible(int id, String user);

	/**
	 * Indicate the given project is visible by the given user, without loading it: to use when only the visibility
	 * is needed.
	 *
	 * @param id   The project's identifier to match.
	 * @param user The current username.
	 * @return <code>true</code> when the project exists and is visible.
	 */
	@Query("SELECT COUNT(p.id) > 0 FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.id = :id AND "
			+ VISIBLE_PROJECTS)
	boolean isVisible(int id, String user);

	/**
	 * Return a project by its primary key. The constraints are:
	 * <ul>
	 * <li>Either <code>user</code> is a system administrator</li>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either <code>user</code> is member of the group associated to this project via the CacheGroup</li>
	 * </ul>
	 *
	 * @param pkey The project primary key to match.
	 * @param user The principal username.
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
	 * @param pkey The primary key to match.
	 * @param user The principal username.
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
	 * <code>canSubscribe</code>, and:
	 * <ul>
	 * <li>Either <code>user</code> is the team leader</li>
	 * <li>Either the project is visible by <code>user</code> and also <code>user</code> has at least one
	 * {@link org.ligoj.app.iam.model.DelegateOrg} with <code>canWrite</code> and related to the group associated to
	 * <code>project</code></li>
	 * </ul>
	 * </li>
	 * </ul>
	 * Note, this will only authorize the principal to create subscriptions to this project, and the valid subscribed
	 * {@link Node}s should be filtered regarding the delegates on this node.
	 *
	 * @param project The project's identifier to match.
	 * @param user    The principal username.
	 * @return <code>true</code> when <code>user</code> can manage the subscriptions of this project.
	 */
	@Query("SELECT COUNT(p.id) > 0 FROM Project AS p LEFT JOIN p.cacheGroups AS cpg0 LEFT JOIN cpg0.group AS cg0 WHERE p.id = :project AND ("
			+ SystemUser.IS_ADMIN + "                          " + "  OR (EXISTS(SELECT 1 FROM DelegateNode d WHERE "
			+ DelegateOrgRepository.ASSIGNED_DELEGATE_D
			+ "   AND d.canSubscribe = true)                                      "
			+ "  AND (p.teamLeader = :user                                      "
			+ "   OR (EXISTS(SELECT 1 FROM DelegateOrg dz WHERE " + DelegateOrgRepository.ASSIGNED_DELEGATE_DZ
			+ "    AND dz.canWrite=true                                      "
			+ "    AND ((dz.type=org.ligoj.app.iam.model.DelegateType.GROUP AND dz.name=cg0.id) OR"
			+ "      (dz.type=org.ligoj.app.iam.model.DelegateType.TREE"
			+ "       AND (RIGHT(cg0.description, LENGTH(dz.dn)+1)=CONCAT(',',dz.dn) OR dz.dn=cg0.description))))))))")
	boolean isManageSubscription(int project, String user);
}

package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.model.Project;

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
	String VISIBLE_PROJECTS = "(" + DelegateOrgRepository.IS_ADMIN
			+ " OR visibleproject(p, cg.description, :user, :user, :user, :user, :user) = true)";

	/**
	 * Return all {@link Project} objects with the given name.The other constraints are :
	 * <ul>
	 * <li>The given user is the team leader</li>
	 * <li>Or, the given user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the given user can see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param user
	 *            The principal user name
	 * @param criteria
	 *            the optional criteria to match.
	 * @param page
	 *            the pagination.
	 * @return all {@link Project} objects with the given name. Insensitive case search is used.
	 */
	@Query(value = "SELECT p, COUNT(DISTINCT s.id) FROM Project AS p LEFT JOIN p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg"
			+ " WHERE " + VISIBLE_PROJECTS + " AND (UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "       OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) GROUP BY p", countQuery = "SELECT COUNT(DISTINCT p) FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg"
					+ " WHERE " + VISIBLE_PROJECTS + " AND (UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
					+ "       OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) GROUP BY p")
	Page<Object[]> findAllLight(String user, String criteria, Pageable page);

	/**
	 * Return all {@link Project} objects having at least one subscription and with light information. The visibility is
	 * checked :
	 * <ul>
	 * <li>The given user is the team leader</li>
	 * <li>Or, the given user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the given user can see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param user
	 *            The principal user name
	 * @return all visible {@link Project} objects for the given user.
	 */
	@Query("SELECT DISTINCT p.id, p.name, p.pkey FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE "
			+ VISIBLE_PROJECTS + " AND EXISTS(SELECT 1 FROM Subscription AS s WHERE s.project=p)")
	List<Object[]> findAllHavingSubscription(String user);

	/**
	 * Return a project by its identifier. The other constraints are :
	 * <ul>
	 * <li>The given user is the team leader</li>
	 * <li>Or, the given user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the given user can see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param id
	 *            the identifier to match.
	 * @param user
	 *            The current user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT DISTINCT p FROM Project AS p LEFT JOIN FETCH p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.id = :id AND "
			+ VISIBLE_PROJECTS)
	Project findOneVisible(int id, String user);

	/**
	 * Return a project by its pkey. The other constraints are :
	 * <ul>
	 * <li>The given user is the team leader</li>
	 * <li>Or, the given user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the given user can see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param pkey
	 *            the pkey to match.
	 * @param user
	 *            The principal user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT DISTINCT p FROM Project AS p LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg WHERE p.pkey = :pkey AND "
			+ VISIBLE_PROJECTS)
	Project findByPKey(String pkey, String user);

	/**
	 * Indicate the given user can manage the subscriptions of the given project. The other constraints are :
	 * <ul>
	 * <li>The given user is the team leader</li>
	 * <li>Or, the given user is an administrator
	 * <li>Or, the given user <strong>canWrite</strong> the group associated to this project via the
	 * <code>service:id</code> subscription and {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param project
	 *            The project's identifier to match.
	 * @param user
	 *            The current user name.
	 * @return Non <code>null</code> project's identifier if the user can manage the subscriptions of this project.
	 * @see org.ligoj.app.iam.model.AbstractDelegate#isCanWrite()
	 */
	@Query("SELECT p.id FROM Project AS p WHERE p.id = :project AND (p.teamLeader = :user OR "
			+ DelegateOrgRepository.IS_ADMIN + " OR EXISTS(SELECT 1 FROM ParameterValue AS pv, CacheGroup g WHERE"
			+ "      pv.parameter.id = 'service:id:group' AND pv.subscription.project = p AND g.id = pv.data"
			+ "  AND (EXISTS(SELECT 1 FROM DelegateOrg d WHERE " + DelegateOrgRepository.ASSIGNED_DELEGATE
			+ "   AND d.canWrite=true AND ((d.type=org.ligoj.app.iam.model.DelegateType.GROUP AND d.dn=g.description) OR"
			+ "    (d.type=org.ligoj.app.iam.model.DelegateType.TREE AND (g.description LIKE CONCAT('%,',d.dn) OR d.dn=g.description)))))))")
	Integer isManageSubscription(int project, String user);
}

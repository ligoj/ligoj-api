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
	 * Visible projects condition where the current user is either team leader,
	 * either member of one of the groups of this project.
	 */
	String MY_PROJECTS = "(p.teamLeader = :user"
			+ " OR EXISTS(SELECT 1 FROM ParameterValue AS pv, CacheGroup g WHERE pv.parameter.id = 'service:id:group' AND pv.subscription.project = p AND g.id = pv.data"
			+ "     AND EXISTS(SELECT 1 FROM CacheMembership AS cm WHERE cm.user.id = :user AND cm.group = g)))";

	/**
	 * Visible projects condition, using ID subscription and team leader
	 * attribute.
	 */
	String VISIBLE_PROJECTS = "(p.teamLeader = :user OR " + DelegateOrgRepository.IS_ADMIN
			+ " OR EXISTS(SELECT 1 FROM CacheProjectGroup pg INNER JOIN pg.group pgg WHERE pg.project = p"
			+ "     AND (EXISTS(SELECT 1 FROM CacheMembership AS cm WHERE cm.group = pgg AND cm.user.id = :user)"
			+ "       OR EXISTS(SELECT 1 FROM DelegateOrg d WHERE " + DelegateOrgRepository.ASSIGNED_DELEGATE
			+ "         AND (pgg.description LIKE CONCAT('%,',d.dn) OR pgg.description=d.dn)))))";


	/**
	 * ":user" : Context user login <br>
	 * <br>
	 * Delegates assigned to a receiver<br>
	 */
	String ASSIGNED_DELEGATE2 = "((receiverType=org.ligoj.app.iam.model.ReceiverType.USER    AND receiver=:user)"
			+ "  OR (receiverType=org.ligoj.app.iam.model.ReceiverType.GROUP   AND EXISTS(SELECT 1 FROM CacheGroup cg   WHERE receiver = cg.id"
			+ "     AND EXISTS(SELECT 1 FROM CacheMembership cm INNER JOIN cm.group g WHERE cm.user.id = :user"
			+ "          AND (g.description = cg.description OR g.description LIKE CONCAT('%,',cg.description)))))"
			+ "  OR (receiverType=org.ligoj.app.iam.model.ReceiverType.COMPANY AND EXISTS(SELECT 1 FROM CacheCompany cc WHERE receiver = cc.id"
			+ "     AND EXISTS(SELECT 1 FROM CacheUser cu INNER JOIN cu.company c   WHERE cu.id = :user"
			+ "          AND (c.description = cc.description OR c.description LIKE CONCAT('%,',cc.description))))))";

	/**
	 * Visible projects condition, using ID subscription and team leader
	 * attribute.
	 */
	String VISIBLE_PROJECTS2 = "(p.teamLeader = :user OR " + DelegateOrgRepository.IS_ADMIN
			+ " OR (EXISTS (SELECT 1 FROM (SELECT cm.group FROM CacheMembership AS cm WHERE cm.user=:user) AS cmg WHERE cmg.group=cpg.group))"
			+ " OR (EXISTS (SELECT 1 FROM (SELECT cm.group FROM DelegateOrg AS d1 WHERE d1.receiverType=org.ligoj.app.iam.model.ReceiverType.USER AND d1.receiver=:user) AS d WHERE cg.description LIKE CONCAT('%,',d.dn) OR cg.description=d.dn))"
			+ " OR (EXISTS (SELECT 1 FROM (SELECT cm.group FROM DelegateOrg AS d1 WHERE d1.receiverType=org.ligoj.app.iam.model.ReceiverType.GROUP"
			+ "			 AND (EXISTS (SELECT 1 FROM (SELECT cg.description AS dn FROM CacheMembership AS cm LEFT JOIN cm.group AS cg WHERE cm.user=:user AND receiver=cg.id) AS cg WHERE cg.description LIKE CONCAT('%,',d.dn) OR cg.description=d.dn)))))"
			+")";

	
	/**
	 * Return all {@link Project} objects with the given name.The other
	 * constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this
	 * project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project
	 * via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param user
	 *            The current user name
	 * @param criteria
	 *            the optional criteria to match.
	 * @param page
	 *            the pagination.
	 * @return all {@link Project} objects with the given name. Insensitive case
	 *         search is used.
	 */
	@Query("SELECT p, COUNT(s.id) FROM Project AS p LEFT JOIN p.subscriptions AS s LEFT JOIN p.cacheGroups AS cpg LEFT JOIN cpg.group AS cg"
			+ " WHERE VISIBLEPROJECT(p.teamLeader, cg.description, :user, :user, :user, :user, :user) = true"
			+ " AND (:criteria IS NULL OR (UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "       OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%')))) GROUP BY p")
	Page<Object[]> findAllLight(String user, String criteria, Pageable page);

	/**
	 * Return all {@link Project} objects having at least one subscription and
	 * with light information. The visibility is checked :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this
	 * project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project
	 * via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param user
	 *            The current user name
	 * @return all visible {@link Project} objects for the given user.
	 */
	@Query("SELECT id, name, pkey FROM Project AS p WHERE " + VISIBLE_PROJECTS
			+ " AND EXISTS(SELECT 1 FROM Subscription AS s WHERE s.project=p)")
	List<Object[]> findAllHavingSubscription(String user);

	/**
	 * Return a project by its identifier. The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this
	 * project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project
	 * via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param id
	 *            the identifier to match.
	 * @param user
	 *            The current user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT p FROM Project AS p LEFT JOIN FETCH p.subscriptions AS s WHERE p.id = :id AND " + VISIBLE_PROJECTS)
	Project findOneVisible(int id, String user);

	/**
	 * Return a project by its pkey. The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this
	 * project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project
	 * via the service:id subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @param pkey
	 *            the pkey to match.
	 * @param user
	 *            The current user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT p FROM Project AS p WHERE p.pkey = :pkey AND " + VISIBLE_PROJECTS)
	Project findByPKey(String pkey, String user);

	/**
	 * Indicate the current user can manage the subscriptions of the given
	 * project. The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is an administrator
	 * <li>Or, the current user <strong>manages</strong> the group associated to
	 * this project via the <code>service:id</code> subscription and
	 * {@link org.ligoj.app.iam.model.DelegateOrg}</li>
	 * </ul>
	 * 
	 * @see org.ligoj.app.iam.model.AbstractDelegate#isCanAdmin()
	 * @param user
	 *            The current user name.
	 * @param project
	 *            The project's identifier to match.
	 * @return Non <code>null</code> project's identifier if the user can manage
	 *         the subscriptions of this project.
	 */
	@Query("SELECT p.id FROM Project AS p WHERE p.id = :project AND (p.teamLeader = :user OR "
			+ DelegateOrgRepository.IS_ADMIN + " OR EXISTS(SELECT 1 FROM ParameterValue AS pv, CacheGroup g WHERE"
			+ "      pv.parameter.id = 'service:id:group' AND pv.subscription.project = p AND g.id = pv.data"
			+ "  AND (EXISTS(SELECT 1 FROM DelegateOrg d WHERE " + DelegateOrgRepository.ASSIGNED_DELEGATE
			+ "   AND d.canAdmin=true AND ((d.type=org.ligoj.app.iam.model.DelegateType.GROUP AND d.dn=g.description) OR"
			+ "    (d.type=org.ligoj.app.iam.model.DelegateType.TREE AND (g.description LIKE CONCAT('%,',d.dn) OR d.dn=g.description)))))))")
	Integer isManageSubscription(int project, String user);

	/**
	 * Return all couples {@link Project} and subscribed group.
	 * 
	 * @return all couples {@link Project} and subscribed group identifier.
	 */
	@Query("SELECT p.id, pv.data FROM ParameterValue pv INNER JOIN pv.subscription AS s"
			+ " INNER JOIN s.project AS p WHERE pv.parameter.id = 'service:id:group'")
	List<Object[]> findAllProjectGroup();
}

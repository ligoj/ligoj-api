package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.dao.ldap.DelegateLdapRepository;
import org.ligoj.app.model.Project;

/**
 * {@link Project} repository
 */
public interface ProjectRepository extends RestRepository<Project, Integer> {

	/**
	 * Visible projects condition where the current user is either team leader, either member of one of the groups of
	 * this project.
	 */
	String MY_PROJECTS = "(p.teamLeader = :user"
			+ " OR EXISTS(SELECT 1 FROM ParameterValue AS pv, CacheGroup g WHERE pv.parameter.id = 'service:id:group' AND pv.subscription.project = p AND g.id = pv.data"
			+ "     AND EXISTS(SELECT 1 FROM CacheMembership AS cm WHERE cm.user.id = :user AND cm.group = g)))";

	/**
	 * Current user is an administrator.
	 */
	String IS_ADMIN = "(EXISTS(SELECT 1 FROM SystemRoleAssignment ra INNER JOIN ra.role r WHERE ra.user = :user"
			+ "     AND EXISTS(SELECT 1 FROM SystemAuthorization a WHERE a.role = r AND a.pattern = '.*'"
			+ "          AND a.type = org.ligoj.bootstrap.model.system.SystemAuthorization$AuthorizationType.BUSINESS)))";

	/**
	 * Visible projects condition, using ID subscription and team leader attribute.
	 */
	String VISIBLE_PROJECTS = "(p.teamLeader = :user OR " + IS_ADMIN
			+ " OR EXISTS(SELECT 1 FROM SystemRoleAssignment ra INNER JOIN ra.role r WHERE ra.user = :user"
			+ "     AND EXISTS(SELECT 1 FROM SystemAuthorization a WHERE a.role = r AND a.pattern = '.*'"
			+ "          AND a.type = org.ligoj.bootstrap.model.system.SystemAuthorization$AuthorizationType.BUSINESS))"
			+ " OR EXISTS(SELECT 1 FROM ParameterValue AS pv, CacheGroup g WHERE pv.parameter.id = 'service:id:group' AND pv.subscription.project = p AND g.id = pv.data"
			+ "     AND (EXISTS(SELECT 1 FROM CacheMembership AS cm WHERE cm.user.id = :user AND cm.group = g)"
			+ "       OR EXISTS(SELECT 1 FROM DelegateLdap d WHERE " + DelegateLdapRepository.ASSIGNED_DELEGATE
			+ " AND ((d.type = org.ligoj.app.model.ldap.DelegateLdapType.GROUP AND d.dn=g.description)"
			+ "                   OR (d.type=org.ligoj.app.model.ldap.DelegateLdapType.TREE AND (g.description LIKE CONCAT('%,',d.dn) OR d.dn=g.description)))))))";

	/**
	 * Return all {@link Project} objects with the given name.The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.model.DelegateLdap}</li>
	 * </ul>
	 * 
	 * @param user
	 *            The current user name
	 * @param criteria
	 *            the optional criteria to match.
	 * @param page
	 *            the pagination.
	 * @return all {@link Project} objects with the given name. Insensitive case search is used.
	 */
	@Query("SELECT p, COUNT(s.id) FROM Project AS p LEFT JOIN p.subscriptions AS s WHERE " + VISIBLE_PROJECTS
			+ " AND (:criteria IS NULL OR (UPPER(p.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "       OR UPPER(p.description) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%')))) GROUP BY p")
	Page<Object[]> findAllLight(String user, String criteria, Pageable page);

	/**
	 * Return all {@link Project} objects having at least one subscription and with light information. The visibility is
	 * checked :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.model.DelegateLdap}</li>
	 * </ul>
	 * 
	 * @param user
	 *            The current user name
	 * @return all visible {@link Project} objects for the given user.
	 */
	@Query("SELECT id, name, pkey FROM Project AS p WHERE EXISTS(SELECT 1 FROM Subscription AS s WHERE s.project=p AND" + VISIBLE_PROJECTS + ")")
	List<Object[]> findAllHavingSubscription(String user);

	/**
	 * Return a project by its identifier. The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.model.DelegateLdap}</li>
	 * </ul>
	 * 
	 * @param id
	 *            the identifier to match.
	 * @param user
	 *            The current user name.
	 * @return the project or <code>null</code> if not found or not visible.
	 */
	@Query("SELECT p FROM Project AS p WHERE p.id = :id AND " + VISIBLE_PROJECTS)
	Project findOneVisible(int id, String user);

	/**
	 * Return a project by its pkey. The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is member of the group associated to this project via the service:id subscription</li>
	 * <li>Or, the current user is see the the group associated to this project via the service:id subscription and
	 * {@link org.ligoj.app.model.DelegateLdap}</li>
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
	 * Indicate the current user can manage the subscriptions of the given project. The other constraints are :
	 * <ul>
	 * <li>The current user is the team leader</li>
	 * <li>Or, the current user is an administrator
	 * <li>Or, the current user <strong>manage</strong> the group associated to this project via the
	 * <code>service:id</code>
	 * subscription and {@link org.ligoj.app.model.DelegateLdap}</li>
	 * </ul>
	 * 
	 * @see org.ligoj.app.model.DelegateLdap#isCanAdmin()
	 * @param user
	 *            The current user name.
	 * @param project
	 *            the project's identifier to match.
	 * @return Non <code>null</code> project identifier if the user can manage the subscriptions of this project.
	 */
	@Query("SELECT p.id FROM Project AS p WHERE p.id = :project AND (p.teamLeader = :user OR " + IS_ADMIN + " OR "
			+ " EXISTS(SELECT 1 FROM ParameterValue AS pv, CacheGroup g WHERE pv.parameter.id = 'service:id:group' AND pv.subscription.project = p AND g.id = pv.data AND "
			+ " (EXISTS(SELECT 1 FROM DelegateLdap d WHERE " + DelegateLdapRepository.ASSIGNED_DELEGATE
			+ " AND d.canAdmin=true AND ((d.type=org.ligoj.app.model.ldap.DelegateLdapType.GROUP AND d.dn=g.description) OR"
			+ " (d.type=org.ligoj.app.model.ldap.DelegateLdapType.TREE AND (g.description LIKE CONCAT('%,',d.dn) OR d.dn=g.description)))))))")
	Integer isManageSubscription(int project, String user);

}

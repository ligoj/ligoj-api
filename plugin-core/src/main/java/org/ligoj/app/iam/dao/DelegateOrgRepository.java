package org.ligoj.app.iam.dao;

import java.util.List;

import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link DelegateOrg} repository.
 */
public interface DelegateOrgRepository extends RestRepository<DelegateOrg, Integer> {

	/**
	 * Current user is an administrator.
	 */
	String IS_ADMIN = "(EXISTS(SELECT 1 FROM SystemRoleAssignment ra INNER JOIN ra.role r WHERE ra.user.id = :user"
			+ "     AND EXISTS(SELECT 1 FROM SystemAuthorization a WHERE a.role = r AND a.pattern = '.*'"
			+ "          AND a.type = org.ligoj.bootstrap.model.system.SystemAuthorization$AuthorizationType.BUSINESS)))";

	/**
	 * ":user" : Context user login <br>
	 * <br>
	 * Delegates assigned to a receiver<br>
	 */
	String ASSIGNED_DELEGATE = "((receiverType=org.ligoj.app.iam.model.ReceiverType.USER    AND receiver=:user)"
			+ "  OR (receiverType=org.ligoj.app.iam.model.ReceiverType.GROUP   AND EXISTS(SELECT 1 FROM CacheGroup cg   WHERE receiver = cg.id"
			+ "     AND EXISTS(SELECT 1 FROM CacheMembership cm INNER JOIN cm.group g WHERE cm.user.id = :user"
			+ "          AND (g.description = cg.description OR g.description LIKE CONCAT('%,',cg.description)))))"
			+ "  OR (receiverType=org.ligoj.app.iam.model.ReceiverType.COMPANY AND EXISTS(SELECT 1 FROM CacheCompany cc WHERE receiver = cc.id"
			+ "     AND EXISTS(SELECT 1 FROM CacheUser cu INNER JOIN cu.company c   WHERE cu.id = :user"
			+ "          AND (c.description = cc.description OR c.description LIKE CONCAT('%,',cc.description))))))";

	/**
	 * ":user" : Context user login <br>
	 * "d" : Current DN <br>
	 * Visible delegates (may not be assigned) for a given user. A assigned
	 * delegates and delegates within the scope of the assigned one are visible.
	 */
	String VISIBLE_DELEGATE = "(" + IS_ADMIN + " OR (" + ASSIGNED_DELEGATE + ")                                      "
			+ "  OR EXISTS (SELECT dz.id FROM DelegateOrg dz WHERE " + ASSIGNED_DELEGATE
			+ "	   AND (dz.type=d.type OR dz.type=org.ligoj.app.iam.model.DelegateType.TREE)                         "
			+ "	   AND (dz.canAdmin=true AND (d.dn LIKE CONCAT('%,',dz.dn) OR dz.dn=d.dn))))                         ";
	/**
	 * ":type" : Type of resource <br>
	 * <br>
	 * Match Type
	 */
	String MATCH_TYPE = "(type=:type OR type=org.ligoj.app.iam.model.DelegateType.TREE)";

	/**
	 * ":type" : Type of resource <br>
	 * ":user" : Context user login <br>
	 * Match Type and user
	 */
	String MATCH_DELEGATE = "(" + MATCH_TYPE + " AND " + ASSIGNED_DELEGATE + ")";

	/**
	 * "l" : Current resource<br>
	 * <br>
	 * Match DN
	 */
	String MATCH_RESOURCE_DN = "(l.description LIKE CONCAT('%,',dn) OR l.description=dn)";

	/**
	 * ":dn" : Current DN<br>
	 * <br>
	 * Match DN
	 */
	String MATCH_DN = "(:dn LIKE CONCAT('%,',dn) OR dn=:dn)";

	/**
	 * ":dn" : Current DN<br>
	 * ":type" : Type of resource <br>
	 * ":user" : Context user login <br>
	 * <br>
	 * Match Type and user and DN
	 */
	String MATCH_DELEGATE_DN = "(" + MATCH_DELEGATE + " AND " + MATCH_DN + ")";

	/**
	 * Return all delegates attached to the given user.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @return The {@link DelegateOrg} of a given user.
	 */
	@Query("FROM DelegateOrg WHERE " + ASSIGNED_DELEGATE)
	List<DelegateOrg> findAllByUser(String user);

	/**
	 * Return <code>true</code> when there is at least one {@link DelegateOrg}
	 * granting the administration right for given user to modify something
	 * within the given DN.
	 * 
	 * @param user
	 *            The user name requesting the operation.
	 * @param dn
	 *            The DN user wants to create.
	 * @param type
	 *            The involved {@link DelegateType}.
	 * @return A positive number if the given DN can be created by the given
	 *         user.
	 */
	@Query("SELECT COUNT(d)>0 FROM DelegateOrg d WHERE (" + IS_ADMIN + " OR canAdmin=true) AND " + MATCH_DELEGATE_DN)
	boolean isAdmin(String user, String dn, DelegateType type);

	/**
	 * Return all {@link DelegateOrg} objects regarding the given criteria.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param criteria
	 *            Optional, use to filter by reveiver's name of delegate's name.
	 * @param type
	 *            Optional {@link DelegateType} to match.
	 * @param page
	 *            The pagination.
	 * @return All {@link DelegateOrg} objects with the given name. Insensitive
	 *         case search is used.
	 */
	@Query("SELECT d FROM DelegateOrg d WHERE " + VISIBLE_DELEGATE
			+ " AND (:type IS NULL OR d.type = :type)                                                                "
			+ "	AND (:criteria = ''                                                                               "
			+ "  OR   UPPER(d.receiver) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))                                "
			+ "  OR   UPPER(d.name)     LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "  OR   (d.type=org.ligoj.app.iam.model.DelegateType.TREE"
			+ "   AND UPPER(d.dn)       LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))")
	Page<DelegateOrg> findAll(String user, String criteria, DelegateType type, Pageable page);

	/**
	 * Return delegate identifiers matching to the given DN
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param dn
	 *            the DN to write.
	 * @param type
	 *            the DN type.
	 * @return delegate identifiers matching to the given DN
	 */
	@Query("SELECT id FROM DelegateOrg WHERE (" + IS_ADMIN + " OR canWrite=true) AND " + MATCH_DELEGATE_DN)
	List<Integer> findByMatchingDnForWrite(String user, String dn, DelegateType type);

	/**
	 * Return delegate identifiers matching to the given DN
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param dn
	 *            The DN to write.
	 * @param type
	 *            The DN type.
	 * @return delegate identifiers matching to the given DN
	 */
	@Query("SELECT id FROM DelegateOrg WHERE (" + IS_ADMIN + " OR canAdmin=true) AND " + MATCH_DELEGATE_DN)
	List<Integer> findByMatchingDnForAdmin(String user, String dn, DelegateType type);

}

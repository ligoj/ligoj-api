package org.ligoj.app.dao.ldap;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.model.DelegateLdap;
import org.ligoj.app.model.DelegateLdapType;

/**
 * {@link DelegateLdap} repository.
 */
public interface DelegateLdapRepository extends RestRepository<DelegateLdap, Integer> {

	/**
	 * ":user" : Context user login <br>
	 * <br>
	 * Delegates assigned to a receiver<br>
	 */
	String ASSIGNED_DELEGATE = "((receiverType=org.ligoj.app.model.ReceiverType.USER    AND receiver=:user)"
			+ "               OR (receiverType=org.ligoj.app.model.ReceiverType.GROUP   AND EXISTS(SELECT 1 FROM CacheGroup cg   WHERE receiver = cg.id"
			+ "                              AND EXISTS(SELECT 1 FROM CacheMembership cm INNER JOIN cm.group g WHERE cm.user.id = :user"
			+ "                                          AND (g.description = cg.description OR g.description LIKE CONCAT('%,',cg.description)))))"
			+ "               OR (receiverType=org.ligoj.app.model.ReceiverType.COMPANY AND EXISTS(SELECT 1 FROM CacheCompany cc WHERE receiver = cc.id"
			+ "                                AND EXISTS(SELECT 1 FROM CacheUser cu INNER JOIN cu.company c   WHERE cu.id = :user"
			+ "                                          AND (c.description = cc.description OR c.description LIKE CONCAT('%,',cc.description))))))";

	/**
	 * ":user" : Context user login <br>
	 * "d" : Current DN <br>
	 * Visible delegates (may not be assigned) for a given user. A assigned delegates and delegates within the scope of
	 * the assigned one are visible.
	 */
	String VISIBLE_DELEGATE = "((" + ASSIGNED_DELEGATE + ")                                                             "
			+ "  OR EXISTS (SELECT dz.id FROM DelegateLdap dz WHERE " + ASSIGNED_DELEGATE
			+ "	   AND (dz.type=d.type OR dz.type=org.ligoj.app.model.ldap.DelegateLdapType.TREE)                         "
			+ "	   AND (dz.canAdmin=true AND (d.dn LIKE CONCAT('%,',dz.dn) OR dz.dn=d.dn))))                            ";
	/**
	 * ":type" : Type of LDAP resource <br>
	 * <br>
	 * Match Type
	 */
	String MATCH_TYPE = "(type=:type OR type=org.ligoj.app.model.ldap.DelegateLdapType.TREE)";

	/**
	 * ":type" : Type of LDAP resource <br>
	 * ":user" : Context user login <br>
	 * Match Type and user
	 */
	String MATCH_DELEGATE = "(" + MATCH_TYPE + " AND " + ASSIGNED_DELEGATE + ")";

	/**
	 * "l" : Current LDAP resource<br>
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
	 * ":type" : Type of LDAP resource <br>
	 * ":user" : Context user login <br>
	 * <br>
	 * Match Type and user and DN
	 */
	String MATCH_DELEGATE_DN = "(" + MATCH_DELEGATE + " AND " + MATCH_DN + ")";

	/**
	 * Return managed LDAP aliases of a given user.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param type
	 *            Delegate type.
	 * @return the managed LDAP aliases of a given user.
	 */
	@Query("SELECT name FROM DelegateLdap WHERE type=:type AND " + ASSIGNED_DELEGATE + " ORDER BY name")
	List<String> findAllNamesByUser(String user, DelegateLdapType type);

	/**
	 * Return all delegates attached to the given user.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @return the {@link DelegateLdap} of a given user.
	 */
	@Query("FROM DelegateLdap WHERE " + ASSIGNED_DELEGATE)
	List<DelegateLdap> findAllByUser(String user);

	/**
	 * Return DN of delegates managing a specific type with write access.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param type
	 *            Delegate type.
	 * @return the managed LDAP aliases of a given user.
	 */
	@Query("SELECT dn FROM DelegateLdap WHERE canWrite=true AND " + MATCH_DELEGATE)
	List<String> findAllDnByUserForWrite(String user, DelegateLdapType type);

	/**
	 * Return DN of delegates managing a specific type with administration access.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param type
	 *            Delegate type.
	 * @return the managed LDAP aliases of a given user.
	 */
	@Query("SELECT dn FROM DelegateLdap WHERE canAdmin=true AND " + MATCH_DELEGATE)
	List<String> findAllDnByUserForAdmin(String user, DelegateLdapType type);

	/**
	 * Return <code>true</code> when there is at least one {@link DelegateLdap} granting the administration right for
	 * given user to modify something within the given DN.
	 * 
	 * @param user
	 *            The user name requesting the operation.
	 * @param dn
	 *            The DN user wants to create.
	 * @param type
	 *            The involved {@link DelegateLdapType}.
	 * @return A positive number if the given DN can be created by the given user.
	 */
	@Query("SELECT COUNT(d)>0 FROM DelegateLdap d WHERE canAdmin=true AND " + MATCH_DELEGATE_DN)
	boolean isAdmin(String user, String dn, DelegateLdapType type);

	/**
	 * Return DN of delegates managing a specific type.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param type
	 *            Delegate type.
	 * @return the managed LDAP aliases of a given user.
	 */
	@Query("SELECT dn FROM DelegateLdap WHERE " + MATCH_DELEGATE)
	List<String> findAllDnByUser(String user, DelegateLdapType type);

	/**
	 * Return all {@link DelegateLdap} objects regarding the given criteria.
	 * 
	 * @param user
	 *            The target user name, receiving the delegation.
	 * @param criteria
	 *            Optional, use to match by LDAP object name or target user.
	 * @param type
	 *            Optional {@link DelegateLdapType} to match..
	 * @param page
	 *            the pagination.
	 * @return all {@link DelegateLdap} objects with the given name. Insensitive case search is used.
	 */
	@Query("SELECT d FROM DelegateLdap d WHERE " + VISIBLE_DELEGATE
			+ " AND (:type IS NULL OR d.type = :type)                                                                    "
			+ "	AND (:criteria IS NULL                                                                                   "
			+ "  OR (UPPER(d.receiver) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))                                     "
			+ "  OR UPPER(d.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "  OR (d.type=org.ligoj.app.model.ldap.DelegateLdapType.TREE AND UPPER(d.dn) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%')))))")
	Page<DelegateLdap> findAll(String user, String criteria, DelegateLdapType type, Pageable page);

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
	@Query("SELECT id FROM DelegateLdap WHERE canWrite=true AND " + MATCH_DELEGATE_DN)
	List<Integer> findByMatchingDnForWrite(String user, String dn, DelegateLdapType type);

	/**
	 * Return a positive number if the given node can be updated or created by the current user. A node can be managed
	 * when it is visible and it exists at least one delegation with administration right for this node or one its
	 * parent.
	 * 
	 * @param user
	 *            The current user name requesting to manage a node.
	 * @param managedUser
	 *            The related user to manage.
	 * @return A positive number if the given user can be managed by the given (requesting) user.
	 */
	@Query("SELECT COUNT(id) FROM DelegateLdap d WHERE d.canWrite=true AND " + ASSIGNED_DELEGATE
			+ " AND ((d.type=org.ligoj.app.model.ldap.DelegateLdapType.COMPANY"
			+ "	       AND EXISTS(SELECT 1 FROM CacheUser u INNER JOIN u.company c WHERE u.id=:managedUser AND c.id = d.name))"
			+ "  OR (d.type=org.ligoj.app.model.ldap.DelegateLdapType.TREE"
			+ "        AND EXISTS(SELECT 1 FROM CacheUser u INNER JOIN u.company c WHERE u.id=:managedUser"
			+ "          AND (c.description LIKE CONCAT('%,',d.dn) OR c.description=d.dn))))")
	int manageUser(String user, String managedUser);

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
	@Query("SELECT id FROM DelegateLdap WHERE canAdmin=true AND " + MATCH_DELEGATE_DN)
	List<Integer> findByMatchingDnForAdmin(String user, String dn, DelegateLdapType type);

}

package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.ligoj.app.dao.ldap.DelegateLdapRepository;
import org.ligoj.app.model.ldap.CacheCompany;
import org.ligoj.app.model.ldap.CacheContainer;

/**
 * {@link CacheCompany} repository
 * @param <C> Cache container type.
 */
public interface CacheRepository<C extends CacheContainer> {

	/**
	 * Partial query, unclosed EXIST and OR for the type of delegate to determine visible delegate.
	 */
	String VISIBLE_DELEGATE_PART_EXISTS_TYPE = "EXISTS(SELECT 1 FROM DelegateLdap d WHERE                                                                "
			+ "      (" + DelegateLdapRepository.MATCH_RESOURCE_DN + " AND " + DelegateLdapRepository.ASSIGNED_DELEGATE + ")                 "
			+ "  AND (type=org.ligoj.app.model.ldap.DelegateLdapType.TREE               ";

	/**
	 * All visible containers regarding the security, and the criteria.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @param criteria
	 *            Optional criteria.
	 * @param page
	 *            Page control.
	 * @return The pagination result.
	 */
	Page<C> findAll(String user, String criteria, Pageable page);

	/**
	 * All visible containers regarding the security.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @return The visible items.
	 */
	List<C> findAll(String user);

	/**
	 * All visible containers regarding the security with write access, and the criteria.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @param criteria
	 *            Optional criteria.
	 * @param page
	 *            Page control.
	 * @return The pagination result.
	 */
	Page<C> findAllWrite(String user, String criteria, Pageable page);

	/**
	 * All visible containers regarding the security with write access.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @return The visible items.
	 */
	List<C> findAllWrite(String user);

	/**
	 * All visible containers regarding the security with administration access, and the criteria.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @param criteria
	 *            Optional criteria.
	 * @param page
	 *            Page control.
	 * @return The pagination result.
	 */
	Page<C> findAllAdmin(String user, String criteria, Pageable page);

	/**
	 * All visible containers regarding the security with administration access.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @return The visible items.
	 */
	List<C> findAllAdmin(String user);

	/**
	 * Return a container matching to the given identifier and also visible by the given user.
	 * 
	 * @param user
	 *            The user requesting the operation.
	 * @param id
	 *            The container's identifier to find.
	 * @return a container matching to the given identifier and also visible by the given user. May be <code>null</code>
	 */
	C findById(String user, String id);

}

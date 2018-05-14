/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.dao;

import java.util.List;

import org.ligoj.app.iam.model.CacheContainer;
import org.ligoj.bootstrap.dao.system.SystemUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link CacheContainer} base repository
 *
 * @param <C>
 *            Cache container type.
 */
public interface CacheContainerRepository<C extends CacheContainer> {

	/**
	 * Filter to determine the company is writable : brought only by a delegate.
	 */
	String WRITABLE_RESOURCE = "(" + SystemUserRepository.IS_ADMIN
			+ " OR writedn(l.description,:user,:user,:user)=true)";

	/**
	 * Filter to determine the group is administered : brought only by a delegate.
	 */
	String ADMIN_RESOURCE = "(" + SystemUserRepository.IS_ADMIN + " OR admindn(l.description,:user,:user,:user)=true)";

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
	@Query("FROM #{#entityName} l WHERE (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) AND "
			+ WRITABLE_RESOURCE)
	Page<C> findAllWrite(String user, String criteria, Pageable page);

	/**
	 * All visible containers regarding the security with write access.
	 *
	 * @param user
	 *            The user requesting the operation.
	 * @return The visible items.
	 */
	@Query("FROM #{#entityName} l WHERE " + WRITABLE_RESOURCE)
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
	@Query("FROM #{#entityName} l WHERE (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) AND "
			+ ADMIN_RESOURCE)
	Page<C> findAllAdmin(String user, String criteria, Pageable page);

	/**
	 * All visible containers regarding the security with administration access.
	 *
	 * @param user
	 *            The user requesting the operation.
	 * @return The visible items.
	 */
	@Query("FROM #{#entityName} l WHERE " + ADMIN_RESOURCE)
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

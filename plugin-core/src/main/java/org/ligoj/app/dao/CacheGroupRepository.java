package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.model.ldap.CacheGroup;

/**
 * {@link CacheGroup} repository
 */
public interface CacheGroupRepository extends RestRepository<CacheGroup, String>, CacheRepository<CacheGroup> {

	/**
	 * Partial query, unclosed EXIST of delegate to determine visible delegate for group.
	 */
	String VISIBLE_DELEGATE_PART_EXISTS = VISIBLE_DELEGATE_PART_EXISTS_TYPE + " OR type=org.ligoj.app.model.ldap.DelegateLdapType.GROUP)";

	/**
	 * Query to determine the delegate is visible or not.
	 */
	String VISIBLE_DELEGATE = VISIBLE_DELEGATE_PART_EXISTS + ")";

	/**
	 * Query to determine the group is visible or not : brought by a delegate or one of the sub groups the current user
	 * is member.
	 */
	String VISIBLE_RESOURCE = VISIBLE_DELEGATE
			+ " OR EXISTS(SELECT 1 FROM CacheMembership cm INNER JOIN cm.user cu INNER JOIN cm.group g WHERE cu.id=:user               "
			+ "             AND (l.description LIKE CONCAT('%,',g.description) OR l.description=g.description))";

	@Override
	@Query("FROM CacheGroup l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))                       "
			+ " AND (" + VISIBLE_RESOURCE + ")")
	Page<CacheGroup> findAll(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheGroup l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))                       "
			+ " AND (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canWrite=true))")
	Page<CacheGroup> findAllWrite(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheGroup l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))                       "
			+ " AND (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canAdmin=true))")
	Page<CacheGroup> findAllAdmin(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheGroup l WHERE (" + VISIBLE_RESOURCE + ")")
	List<CacheGroup> findAll(String user);

	@Override
	@Query("FROM CacheGroup l WHERE (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canWrite=true))")
	List<CacheGroup> findAllWrite(String user);

	@Override
	@Query("FROM CacheGroup l WHERE (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canAdmin=true))")
	List<CacheGroup> findAllAdmin(String user);

	@Override
	@Query("FROM CacheGroup l WHERE id=:id AND (" + VISIBLE_RESOURCE + ")")
	CacheGroup findById(String user, String id);

}

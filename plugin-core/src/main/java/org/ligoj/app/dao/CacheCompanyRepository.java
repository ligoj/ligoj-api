package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.model.ldap.CacheCompany;

/**
 * {@link CacheCompany} repository
 */
public interface CacheCompanyRepository extends RestRepository<CacheCompany, String>, CacheRepository<CacheCompany> {
	/**
	 * Partial query, unclosed EXIST of delegate to determine visible delegate for company.
	 */
	String VISIBLE_DELEGATE_PART_EXISTS = VISIBLE_DELEGATE_PART_EXISTS_TYPE + " OR type=org.ligoj.app.model.ldap.DelegateLdapType.COMPANY)";

	/**
	 * Query to determine the delegate is visible or not.
	 */
	String VISIBLE_DELEGATE = VISIBLE_DELEGATE_PART_EXISTS + ")";

	/**
	 * Query to determine the company is visible or not : brought by a delegate or one of the sub companies the current
	 * user belongs to.
	 */
	String VISIBLE_RESOURCE = VISIBLE_DELEGATE + " OR EXISTS(SELECT 1 FROM CacheUser u INNER JOIN u.company c WHERE u.id=:user               "
			+ "                                    AND (l.description LIKE CONCAT('%,',c.description) OR l.description=c.description))";

	@Override
	@Query("FROM CacheCompany l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))                       "
			+ " AND (" + VISIBLE_RESOURCE + ")")
	Page<CacheCompany> findAll(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheCompany l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))                       "
			+ " AND (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canWrite=true))")
	Page<CacheCompany> findAllWrite(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheCompany l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))))                       "
			+ " AND (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canAdmin=true))")
	Page<CacheCompany> findAllAdmin(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheCompany l WHERE (" + VISIBLE_RESOURCE + ")")
	List<CacheCompany> findAll(String user);

	@Override
	@Query("FROM CacheCompany l WHERE (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canWrite=true))")
	List<CacheCompany> findAllWrite(String user);

	@Override
	@Query("FROM CacheCompany l WHERE (" + VISIBLE_DELEGATE_PART_EXISTS + "AND canAdmin=true))")
	List<CacheCompany> findAllAdmin(String user);

	@Override
	@Query("FROM CacheCompany l WHERE id=:id AND (" + VISIBLE_RESOURCE + ")")
	CacheCompany findById(String user, String id);

}

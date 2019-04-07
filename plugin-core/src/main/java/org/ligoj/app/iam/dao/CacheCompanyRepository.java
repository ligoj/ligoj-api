/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.dao;

import java.util.List;

import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link CacheCompany} repository
 */
public interface CacheCompanyRepository
		extends RestRepository<CacheCompany, String>, CacheContainerRepository<CacheCompany> {

	/**
	 * Filter to determine the company is visible or not.
	 */
	String VISIBLE_RESOURCE = "(" + SystemUser.IS_ADMIN
			+ " OR visiblecompany(l.description,:user,:user,:user,:user)=true)";

	@Override
	@Query("FROM CacheCompany l WHERE (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))) AND "
			+ VISIBLE_RESOURCE)
	Page<CacheCompany> findAll(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheCompany l WHERE " + VISIBLE_RESOURCE)
	List<CacheCompany> findAll(String user);

	@Override
	@Query("FROM CacheCompany l WHERE id=:id AND " + VISIBLE_RESOURCE)
	CacheCompany findById(String user, String id);

}

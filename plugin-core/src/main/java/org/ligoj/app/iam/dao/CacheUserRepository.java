/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.dao;

import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link CacheUser} repository
 */
@SuppressWarnings("ALL")
public interface CacheUserRepository extends RestRepository<CacheUser, String> {

	/**
	 * Lookup criteria of the system user search extended to the IAM attributes: empty criteria matches all,
	 * otherwise the login or any IAM detail (first name, last name, mails) of the same identifier is matched.
	 * There is no JPA association between these entities — the "ad hoc" entity join on the common primary key
	 * bridges them; a system user without IAM entry is still returned (LEFT JOIN), only matchable by its login.
	 */
	String LOOKUP_CRITERIA = " FROM SystemUser u LEFT JOIN CacheUser cu ON cu.id = u.login"
			+ " WHERE :criteria = ''"
			+ "    OR UPPER(u.login)      LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "    OR UPPER(cu.firstName) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "    OR UPPER(cu.lastName)  LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "    OR UPPER(cu.mails)     LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))";

	/**
	 * Return the {@link SystemUser} page (roles fetched) matching the given criteria against the login or the IAM
	 * details sharing the same identifier. The pagination is applied by the database.
	 *
	 * @param criteria The lookup criteria, or an empty string to match all.
	 * @param page     The pagination/sort information.
	 * @return The matching system users with fetched roles.
	 */
	@Query(value = "SELECT u FROM SystemUser u LEFT JOIN FETCH u.roles ra LEFT JOIN FETCH ra.role"
			+ " LEFT JOIN CacheUser cu ON cu.id = u.login"
			+ " WHERE :criteria = ''"
			+ "    OR UPPER(u.login)      LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "    OR UPPER(cu.firstName) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "    OR UPPER(cu.lastName)  LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))"
			+ "    OR UPPER(cu.mails)     LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))",
			countQuery = "SELECT COUNT(u)" + LOOKUP_CRITERIA)
	Page<SystemUser> findAllSystemUsersByDetails(String criteria, Pageable page);
}

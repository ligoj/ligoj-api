/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import org.ligoj.app.model.UserLog;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * {@link UserLog} repository.
 */
public interface UserLogRepository extends RestRepository<UserLog, Integer> {

	/**
	 * Return all user logs within the given date range. The caller is expected to widen the range to cover the
	 * "no bound" cases (see {@link org.ligoj.app.resource.log.UserLogResource}): both bounds are always provided, which
	 * keeps the generated statement portable (some databases cannot resolve the type of a bare {@code NULL} bind
	 * parameter).
	 *
	 * @param from Lower bound (inclusive).
	 * @param to   Upper bound (inclusive).
	 * @param page The pagination and sort.
	 * @return The matching page of user logs.
	 */
	@Query("FROM UserLog u WHERE u.date >= :from AND u.date <= :to")
	Page<UserLog> findAllByDate(@Param("from") Instant from, @Param("to") Instant to, Pageable page);
}

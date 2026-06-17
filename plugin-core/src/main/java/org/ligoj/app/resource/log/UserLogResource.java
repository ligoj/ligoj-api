/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.log;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.dao.UserLogRepository;
import org.ligoj.app.model.UserLog;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * User log resource. Authenticated users log their own browser side errors with the POST endpoint, while the listing is
 * restricted to administrators.
 */
@Path("/user-log")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class UserLogResource {

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private UserLogRepository repository;

	@Autowired
	private PaginationJson paginationJson;

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORDERED_COLUMNS = Map.of("id", "id", "user", "user", "date", "date",
			"message", "message", "url", "url");

	/**
	 * Non-string ordered columns: no case-insensitive ({@code lower()}) wrapping, which is invalid on numeric/temporal
	 * types.
	 */
	private static final Set<String> CASE_SENSITIVE_COLUMNS = Set.of("id", "date");

	/**
	 * Lower bound used when no <code>from</code> is provided.
	 */
	private static final Instant MIN_DATE = Instant.EPOCH;

	/**
	 * Upper bound used when no <code>to</code> is provided. Stays within the range supported by SQL timestamps.
	 */
	private static final Instant MAX_DATE = Instant.parse("9999-12-31T23:59:59Z");

	/**
	 * Log a browser side error for the current user. Open to any authenticated user: the login and the date are filled
	 * by the server, so a user can only log on its own behalf.
	 *
	 * @param vo The error to log.
	 */
	@POST
	public void log(final UserLogEditionVo vo) {
		final var entity = new UserLog();
		entity.setUser(securityHelper.getLogin());
		entity.setDate(Instant.now());
		entity.setMessage(StringUtils.truncate(vo.getMessage(), 2000));
		entity.setUrl(vo.getUrl());
		repository.saveAndFlush(entity);
	}

	/**
	 * Retrieve all user logs with pagination and an optional date range. Restricted to administrators.
	 *
	 * @param uriInfo Pagination data.
	 * @param from    Optional lower bound (inclusive) as epoch milliseconds.
	 * @param to      Optional upper bound (inclusive) as epoch milliseconds.
	 * @return All matching logs with pagination.
	 */
	@GET
	@PreAuthorize("hasAuthority('ADMIN')")
	public TableItem<UserLogVo> findAll(@Context final UriInfo uriInfo, @QueryParam("from") final Long from,
			@QueryParam("to") final Long to) {
		final var page = paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS, CASE_SENSITIVE_COLUMNS);
		final var result = repository.findAllByDate(from == null ? MIN_DATE : Instant.ofEpochMilli(from),
				to == null ? MAX_DATE : Instant.ofEpochMilli(to), page);
		return paginationJson.applyPagination(uriInfo, result, this::toVo);
	}

	/**
	 * Converter from {@link UserLog} to {@link UserLogVo}.
	 *
	 * @param entity The entity to convert.
	 * @return The corresponding business object.
	 */
	private UserLogVo toVo(final UserLog entity) {
		final var vo = new UserLogVo();
		vo.setId(entity.getId());
		vo.setUser(entity.getUser());
		vo.setDate(entity.getDate());
		vo.setMessage(entity.getMessage());
		vo.setUrl(entity.getUrl());
		return vo;
	}
}

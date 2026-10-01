/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.dao.DelegateNodeRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Node delegation resource.
 */
@Path("/node/delegate")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class DelegateNodeResource {

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private DelegateNodeRepository repository;

	@Autowired
	private PaginationJson paginationJson;

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORDERED_COLUMNS = new HashMap<>();

	static {
		ORDERED_COLUMNS.put("id", "id");
		ORDERED_COLUMNS.put("name", "name");
		ORDERED_COLUMNS.put("receiver", "receiver");
		ORDERED_COLUMNS.put("receiverType", "receiverType");
		ORDERED_COLUMNS.put("canAdmin", "CAST(canAdmin AS String)");
		ORDERED_COLUMNS.put("canWrite", "CAST(canWrite AS String)");
		ORDERED_COLUMNS.put("canSubscribe", "CAST(canSubscribe AS String)");
	}

	/**
	 * Retrieve all elements with pagination
	 *
	 * @param uriInfo  pagination data.
	 * @param criteria Optional text to match.
	 * @return all elements with pagination.
	 */
	@GET
	public TableItem<DelegateNode> findAll(@Context final UriInfo uriInfo,
			@QueryParam(DataTableAttributes.SEARCH) final String criteria) {
		final var pageRequest = paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS);
		final var findAll = repository.findAll(securityHelper.getLogin(), StringUtils.trimToEmpty(criteria),
				pageRequest);

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, Function.identity());
	}

	/**
	 * Create a delegate. Rules are :
	 * <ul>
	 * <li>Related node must be managed by the current user, directly or via another parent delegate.</li>
	 * <li>'write' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * <li>At least one delegate with 'admin' flag must be present for the current user and the related node.</li>
	 * </ul>
	 * . Target user is not checked.
	 *
	 * @param vo the object to create.
	 * @return the entity's identifier.
	 */
	@POST
	public int create(final DelegateNode vo) {
		// A creation never overwrites an existing delegate
		vo.setId(null);
		return validateSaveOrUpdate(vo).getId();
	}

	/**
	 * Validate the user changes regarding the current user's right. Rules, order is important :
	 * <ul>
	 * <li>Related node must be managed by the current user, directly or via another parent delegate or act as if the
	 * company does not exist.</li>
	 * <li>'write' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * <li>'admin' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * </ul>
	 * Target user is not checked.
	 *
	 * @return Created delegate.
	 */
	private DelegateNode validateSaveOrUpdate(final DelegateNode entity) {
		// Get all delegates of current user
		final var node = entity.getName();

		// Check there is at least one delegate for this user allowing him to update/create this delegate
		if (repository.manageNode(securityHelper.getLogin(), node, entity.isCanWrite()) == 0) {
			throw new NotFoundException();
		}

		return repository.saveAndFlush(entity);
	}

	/**
	 * Update a delegate. Rules are :
	 * <ul>
	 * <li>The replaced delegate, when identified, must be visible and managed by the current user.</li>
	 * <li>Related node must be managed by the current user, directly or via another parent delegate.</li>
	 * <li>'write' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * </ul>
	 * Target user is not checked.
	 *
	 * @param vo the object to update.
	 */
	@PUT
	public void update(final DelegateNode vo) {
		if (vo.getId() != null) {
			// The replaced delegate must be managed too, not only the new one
			final var existing = repository.findOneVisible(vo.getId(), securityHelper.getLogin());
			if (existing == null || !canManage(existing)) {
				throw new NotFoundException();
			}
		}
		validateSaveOrUpdate(vo);
	}

	/**
	 * Indicate the given delegate can be managed by the current user: a delegate with the 'admin' flag on its node or
	 * a parent, and with the 'write' flag when the delegate has it.
	 */
	private boolean canManage(final DelegateNode delegate) {
		return repository.manageNode(securityHelper.getLogin(), delegate.getName(), delegate.isCanWrite()) > 0;
	}

	/**
	 * Return a visible entity by its identifier.
	 *
	 * @param id the entity identifier.
	 * @return A visible entity by its identifier.
	 */
	@GET
	@Path("{id:\\d+}")
	public DelegateNode findById(@PathParam("id") final int id) {
		final var entity = repository.findOneVisible(id, securityHelper.getLogin());
		if (entity == null) {
			throw new NotFoundException();
		}
		return entity;
	}

	/**
	 * Delete entity. Rules, order is important :
	 * <ul>
	 * <li>Related delegate must exist</li>
	 * <li>Related delegate must be managed by the current user with 'admin' right, directly or via another parent or
	 * act as if the delegate does not exist.</li>
	 * </ul>
	 *
	 * @param id the entity identifier.
	 */
	@DELETE
	@Path("{id:\\d+}")
	public void delete(@PathParam("id") final int id) {
		// Perform the deletion and check the result
		var entity = repository.findById(id, securityHelper.getLogin());
		if (entity == null || !canManage(entity)) {
			throw new NotFoundException();
		}
		repository.delete(entity);
	}
}

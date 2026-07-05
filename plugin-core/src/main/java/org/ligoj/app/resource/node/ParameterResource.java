/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.model.Parameter;
import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Criteria values Business Layer for entity {@link Parameter}
 */
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/node")
public class ParameterResource {

	@Autowired
	private ParameterRepository repository;

	@Autowired
	private SecurityHelper securityHelper;

	/**
	 * Return a parameter attached to a visible node for the current user.
	 *
	 * @param id Parameter identifier.
	 * @return The parameter from its identifier. May be <code>null</code>.
	 */
	public Parameter findByIdInternal(final String id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin()))
				.orElseThrow(EntityNotFoundException::new);
	}

	/**
	 * Return all node parameter definitions where a value is expected to be provided to the final subscription. The
	 * parameters are ordered by identifier, ascending. The UI ({@code SubscribeWizardView}) re-orders them by display
	 * name and applies any plugin-provided ordering/grouping.
	 *
	 * @param node The node identifier.
	 * @param mode Subscription mode.
	 * @return All parameter definitions where a value is expected to be attached to the final subscription in given
	 * mode.
	 */
	@GET
	@Path("{node:service:.+}/parameter/{mode}")
	public List<ParameterVo> getNotProvidedParameters(@PathParam("node") final String node,
			@PathParam("mode") final SubscriptionMode mode) {
		return repository.getOrphanParameters(node, mode, securityHelper.getLogin()).stream().map(NodeHelper::toVo)
				.sorted(Comparator.comparing(AbstractBusinessEntity::getId)).toList();
	}

	/**
	 * Return all node parameter definitions where a value is expected to be provided to the final subscription.
	 *
	 * @param node The node identifier.
	 * @param mode Subscription mode.
	 * @return All parameter definitions where a value is expected to be attached to the final subscription in given
	 * mode.
	 */
	public List<ParameterVo> getNotProvidedAndAssociatedParameters(final String node, final SubscriptionMode mode) {
		return repository.getOrphanParametersExt(node, mode, securityHelper.getLogin()).stream()
				.map(NodeHelper::toVo).toList();
	}
}

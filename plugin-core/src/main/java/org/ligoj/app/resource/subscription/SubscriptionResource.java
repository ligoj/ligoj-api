/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.api.*;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.node.*;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link Subscription} resource.
 */
@Path("/subscription")
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class SubscriptionResource extends AbstractLockedResource<Subscription, Integer> {

	@Autowired
	private SubscriptionRepository repository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private NodeRepository nodeRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private ParameterValueResource parameterValueResource;

	@Autowired
	private EventResource eventResource;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private NodeResource nodeResource;

	/**
	 * {@link SubscriptionEditionVo} to JPA entity transformer.
	 *
	 * @param vo      The object to convert.
	 * @param project The related project.
	 * @param node    The related node.
	 * @return The mapped entity.
	 */
	public static Subscription toEntity(final SubscriptionEditionVo vo, final Project project, final Node node) {
		final var entity = new Subscription();
		entity.setProject(project);
		entity.setId(vo.getId());
		entity.setNode(node);
		entity.setMode(vo.getMode());
		return entity;
	}

	/**
	 * Return non secured parameters values related to the subscription.The attached project is validated against the
	 * current user to check it is visible. Secured parameters (even the encrypted ones) are not returned. The
	 * visibility of this subscription is checked.
	 *
	 * @param id The subscription identifier.
	 * @return secured associated parameters values. Key of returned map is the identifier of
	 * {@link org.ligoj.app.model.Parameter}
	 */
	@GET
	@Path("{id:\\d+}")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Map<String, String> getNonSecuredParameters(@PathParam("id") final int id) {
		return parameterValueResource.getNonSecuredSubscriptionParameters(checkVisible(id).getId());
	}

	/**
	 * Return tools specific configuration. Only non secured parameters are returned.
	 *
	 * @param id The subscription identifier.
	 * @return tools specific configuration.
	 * @throws Exception When the configuration gathering fails. Managed at JAX-RS level.
	 */
	@GET
	@Path("{id:\\d+}/configuration")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public ConfigurationVo getConfiguration(@PathParam("id") final int id) throws Exception {

		// Copy subscription details
		final var entity = checkVisible(id);
		final var vo = new ConfigurationVo();
		vo.setNode(NodeResource.toVo(entity.getNode(), locator));
		vo.setParameters(this.getNonSecuredParameters(id));
		vo.setSubscription(id);
		vo.setProject(DescribedBean.clone(entity.getProject()));

		// Get specific configuration
		final var servicePlugin = locator.getResource(vo.getNode().getId(), ConfigurablePlugin.class);
		if (servicePlugin != null) {
			// Specific configuration is available
			vo.setConfiguration(servicePlugin.getConfiguration(id));
		}
		return vo;
	}

	/**
	 * Return all parameters values related to the subscription. The attached project is validated against the current
	 * user to check it is visible. Beware, these parameters must not be returned to user, since clear encrypted
	 * parameters are present.
	 *
	 * @param id The subscription identifier.
	 * @return all associated parameters values. Key of returned map is the identifier of
	 * {@link org.ligoj.app.model.Parameter}
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Map<String, String> getParameters(final int id) {
		checkVisible(id);
		return this.getParametersNoCheck(id);
	}

	/**
	 * Return all parameters values related to the subscription. The visibility of attached project is not checked in
	 * this case. Secured (encrypted) parameters are decrypted.
	 *
	 * @param id The subscription identifier.
	 * @return all associated parameters values. Key of returned map is the identifier of
	 * {@link org.ligoj.app.model.Parameter}
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Map<String, String> getParametersNoCheck(final int id) {
		return parameterValueResource.getSubscriptionParameters(id);
	}

	/**
	 * Create subscription.
	 *
	 * @param vo the subscription.
	 * @return the created {@link Subscription}.
	 * @throws Exception When the creation fails. Managed at JAX-RS level.
	 */
	@POST
	public int create(final SubscriptionEditionVo vo) throws Exception {

		// Validate entities
		final var project = checkVisibleProject(vo.getProject());
		checkManagedProject(vo.getProject());
		final var node = checkManagedNodeForSubscription(vo.getNode());
		final var acceptedParameters = checkInputParameters(vo);

		// Create subscription and parameters that would be removed in case of
		// roll-back because of invalid parameters
		final var entity = toEntity(vo, project, node);

		// Expose the real entity for plug-in since we have loaded it
		entity.setProject(project);

		// Save this subscription in the transaction
		repository.saveAndFlush(entity);
		parameterValueResource.create(vo.getParameters(), entity);

		// Delegate to the related plug-in the next process
		delegateToPlugin(vo, entity);

		// Check again the parameters in the final state
		checkMandatoryParameters(vo.getParameters(), acceptedParameters, SubscriptionMode.CREATE);
		log.info("Subscription of project {} to service {}", vo.getProject(), vo.getNode());

		return entity.getId();
	}

	/**
	 * Delegates the creation to the hierarchy of the related plug-in, and starting from the related plug-in. <br>
	 * Exception appearing there causes to roll-back the previous persists.
	 *
	 * @throws Exception When the link/create fails. Managed at upper level.
	 */
	private void delegateToPlugin(final SubscriptionEditionVo vo, final Subscription entity) throws Exception {
		for (var p = locator.getResource(vo.getNode()); p != null; p = locator.getResource(locator.getParent(p.getKey()))) {
			if (vo.getMode() == SubscriptionMode.CREATE) {
				// Create mode
				p.create(entity.getId());
			} else {
				// Link mode
				p.link(entity.getId());
			}
		}
	}

	/**
	 * Check the parameters that are being attached to this subscription.
	 */
	private List<Parameter> checkInputParameters(final SubscriptionEditionVo vo) {
		final var acceptedParameters = nodeResource.checkInputParameters(vo);

		// Check all mandatory parameters for the current subscription mode
		vo.setParameters(ObjectUtils.defaultIfNull(vo.getParameters(), new ArrayList<>()));
		checkMandatoryParameters(vo.getParameters(), acceptedParameters, vo.getMode());
		return acceptedParameters;
	}

	/**
	 * Check the principal user can subscribe a project to the given visible node.
	 *
	 * @param node The node identifier to subscribe.
	 * @return The found visible node. Never <code>null</code>.
	 */
	private Node checkManagedNodeForSubscription(final String node) {
		// Check the node can be subscribed by the principal user
		final var entity = Optional.ofNullable(nodeRepository.findOneForSubscription(node, securityHelper.getLogin())).orElseThrow(() -> new ValidationJsonException("node", BusinessException.KEY_UNKNOWN_ID, "0", node));

		// Check the node accept subscription
		if (entity.getMode() == SubscriptionMode.NONE) {
			throw new ValidationJsonException("node", "invalid-mode", "0", node);
		}
		return entity;
	}

	/**
	 * Check mandatory parameters are provided.
	 *
	 * @param parameters         The updated parameters to check.
	 * @param acceptedParameters The accepted parameters.
	 * @param mode               The related mode.
	 */
	protected void checkMandatoryParameters(final List<ParameterValueCreateVo> parameters, final List<Parameter> acceptedParameters, final SubscriptionMode mode) {
		// Check each mandatory parameter for the current mode
		acceptedParameters.stream().filter(parameter -> (parameter.getMode() == mode || parameter.getMode() == SubscriptionMode.ALL) && parameter.isMandatory()).forEach(parameter -> checkMandatoryParameter(parameters, parameter));
	}

	/**
	 * Check mandatory parameter is provided.
	 */
	private void checkMandatoryParameter(final Collection<ParameterValueCreateVo> parameters, final Persistable<String> parameter) {
		// This parameter must exist
		if (parameters.stream().noneMatch(value -> value.getParameter().equals(parameter.getId()))) {
			// Missing mandatory parameter
			throw ValidationJsonException.newValidationJsonException(NotNull.class.getSimpleName(), parameter.getId());
		}
	}

	/**
	 * Delete entity and cascaded associations : parameters, events then subscription. Note that remote data are not
	 * deleted. Links are just destroyed.
	 *
	 * @param id the entity identifier.
	 * @throws Exception When the deletion fails. Managed at JAX-RS level.
	 */
	@Path("{id:\\d+}")
	@DELETE
	public void delete(@PathParam("id") final int id) throws Exception {
		// Deletion without remote deletion
		delete(id, false);
	}

	/**
	 * Delete entity and cascaded associations : parameters, events then subscription.
	 *
	 * @param id               the entity identifier.
	 * @param deleteRemoteData When <code>true</code>, created remote data will be also destroyed. Ignored if the subscription mode is 'LINK'.
	 * @throws Exception When the deletion fails. Managed at JAX-RS level.
	 */
	@Path("{id:\\d+}/{deleteRemoteData}")
	@DELETE
	public void delete(@PathParam("id") final int id, @PathParam("deleteRemoteData") final boolean deleteRemoteData) throws Exception {
		final var entity = checkVisible(id);
		checkManagedProject(entity.getProject().getId());

		// Delete the events
		eventRepository.deleteAllBy("subscription", entity);

		// Delegate the deletion
		deleteWithTasks(entity.getNode().getId(), id, SubscriptionMode.LINK != entity.getMode() && deleteRemoteData);
		parameterValueResource.deleteBySubscription(id);
		repository.delete(entity);
	}

	@Override
	protected void delete(final ServicePlugin plugin, final Integer id, final boolean deleteRemoteData) throws Exception {
		plugin.delete(id, deleteRemoteData);
	}

	/**
	 * Check the associated project is managed for current user. Currently, a managed project is a project where
	 * subscription can be managed.
	 */
	private void checkManagedProject(final int project) {
		if (!projectRepository.isManageSubscription(project, securityHelper.getLogin())) {
			// Not managed associated project
			log.warn("Attempt to manage a project '{}' out of scope", project);
			throw new ForbiddenException();
		}
	}

	/**
	 * Check the associated project is visible for current user.
	 *
	 * @param id Project's identifier.
	 * @return the loaded project.
	 */
	public Project checkVisibleProject(final int id) {
		final var project = projectRepository.findOneVisible(id, securityHelper.getLogin());
		if (project == null) {
			// Associated project is not visible
			throw new EntityNotFoundException(String.valueOf(id));
		}
		return project;
	}

	@Override
	public Subscription checkVisible(final Integer id) {
		final var entity = repository.findOneExpected(id);
		if (projectRepository.findOneVisible(entity.getProject().getId(), securityHelper.getLogin()) == null) {
			// Associated project is not visible, reject the subscription access
			throw new EntityNotFoundException(String.valueOf(id));
		}
		return entity;
	}

	/**
	 * Return all subscriptions and related nodes. Very light data is returned there due the large number of  subscriptions.
	 * Parameters values are not fetch.
	 *
	 * @return Status of each subscription of each project and each node.
	 */
	@GET
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public SubscriptionListVo findAll() {
		final var result = new SubscriptionListVo();

		// First, list visible projects having at least one subscription
		final var projects = projectRepository.findAllHavingSubscription(securityHelper.getLogin());

		// Fill the projects
		final var projectsMap = toProjects(projects);
		result.setProjects(projectsMap.values());

		/*
		 * List visible projects having at least one subscription, return involved subscriptions relating these
		 * projects. SQL "IN" is not used, because of size limitations. Structure : id, project.id, service.id
		 */
		result.setSubscriptions(toSubscriptions(repository.findAllLight(), projectsMap));

		/*
		 * Then, fetch all nodes. SQL "IN" is not used, because of size limitations. They will be filtered against
		 * subscriptions associated to a visible project.
		 */
		result.setNodes(toNodes(nodeResource.findAll(), result.getSubscriptions()).values());
		return result;
	}

	/**
	 * Extract the distinct nodes from the subscriptions.
	 */
	private Map<String, SubscribedNodeVo> toNodes(final Map<String, NodeVo> nodes, final Collection<SubscriptionLightVo> subscriptions) {
		final var filteredNodes = new TreeMap<String, SubscribedNodeVo>();
		// Add the related node of each subscription
		subscriptions.stream().map(SubscriptionLightVo::getNode).map(nodes::get).forEach(n -> addNodeAsNeeded(filteredNodes, nodes, n));
		return filteredNodes;
	}

	/**
	 * Convert the subscriptions result set to {@link SubscriptionLightVo}
	 */
	private Collection<SubscriptionLightVo> toSubscriptions(final List<Object[]> subscriptions, final Map<Integer, SubscribingProjectVo> projects) {
		// Prepare the subscriptions container with project name ordering
		return subscriptions.stream().filter(rs -> projects.containsKey(rs[1])).map(rs -> {
			// Build the subscription data
			final var vo = new SubscriptionLightVo();
			vo.setId((Integer) rs[0]);
			vo.setProject((Integer) rs[1]);
			vo.setNode((String) rs[2]);
			return vo;
		}).collect(() -> new TreeSet<>((o1, o2) -> (projects.get(o1.getProject()).getName() + "," + o1.getId()).compareToIgnoreCase(projects.get(o2.getProject()).getName() + "," + o2.getId())), TreeSet::add, TreeSet::addAll);
	}

	/**
	 * Convert the project result set to {@link SubscribingProjectVo}
	 */
	private Map<Integer, SubscribingProjectVo> toProjects(final List<Object[]> projects) {
		return projects.stream().map(rs -> {
			// Build the project
			final var project = new SubscribingProjectVo();
			project.setId((Integer) rs[0]);
			project.setName((String) rs[1]);
			project.setPkey((String) rs[2]);

			// Also save it for indexed search
			return project;
		}).collect(Collectors.toMap(SubscribingProjectVo::getId, Function.identity()));
	}

	/**
	 * Add a node to the filtered nodes, and also add recursively the parent.
	 */
	private void addNodeAsNeeded(final Map<String, SubscribedNodeVo> filteredNodes, final Map<String, NodeVo> allNodes, final NodeVo node) {
		if (!filteredNodes.containsKey(node.getId())) {

			// Build the node wrapper
			final var subscribedNode = new SubscribedNodeVo();
			NamedBean.copy(node, subscribedNode);
			subscribedNode.setTag(node.getTag());
			subscribedNode.setTagUiClasses(node.getTagUiClasses());
			filteredNodes.put(node.getId(), subscribedNode);

			// Now check the parent exists or not and add it to the target
			// filtered nodes
			if (node.isRefining()) {

				// Completed the previous link
				subscribedNode.setRefined(node.getRefined().getId());

				// Add the parent too (as needed
				addNodeAsNeeded(filteredNodes, allNodes, allNodes.get(subscribedNode.getRefined()));
			}
		}
	}

	/**
	 * Retrieve the last known status of subscriptions of given project .
	 *
	 * @param project project identifier
	 * @return Status of each subscription of given project.
	 */
	@Path("status/{project:\\d+}")
	@GET
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Map<Integer, EventVo> getStatusByProject(@PathParam("project") final int project) {
		return eventRepository.findLastEvents(project).stream().map(EventResource::toVo).collect(Collectors.toMap(EventVo::getSubscription, Function.identity()));
	}

	/**
	 * Get fresh status of given subscription. This fresh status is also stored in the database. The project must be
	 * visible to current user.
	 *
	 * @param id Node identifier
	 * @return Status of each subscription of given project.
	 */
	@Path("status/{id:\\d+}/refresh")
	@GET
	public SubscriptionStatusWithData refreshStatus(@PathParam("id") final int id) {
		return refreshSubscription(checkVisible(id));
	}

	/**
	 * Get fresh status of a set of subscriptions. This a loop shortcut of the per-subscription call.
	 *
	 * @param ids Node identifiers
	 * @return Status of each subscription of given project. Order is not guaranteed.
	 * @see #refreshStatus(int)
	 */
	@Path("status/refresh")
	@GET
	public Map<Integer, SubscriptionStatusWithData> refreshStatuses(@QueryParam("id") final Set<Integer> ids) {
		return ids.stream().map(this::refreshStatus).collect(Collectors.toMap(SubscriptionStatusWithData::getId, Function.identity()));
	}

	/**
	 * Refresh given subscriptions and return their status.
	 */
	private SubscriptionStatusWithData refreshSubscription(final Subscription subscription) {
		final var parameters = this.getParameters(subscription.getId());
		final var statusWithData = nodeResource.checkSubscriptionStatus(subscription, parameters);
		statusWithData.setId(subscription.getId());
		statusWithData.setProject(subscription.getProject().getId());
		statusWithData.setParameters(parameterValueResource.getNonSecuredSubscriptionParameters(subscription.getId()));

		// Update the last event with fresh data
		eventResource.registerEvent(subscription, EventType.STATUS, statusWithData.getStatus().name());

		// Return the fresh statuses
		return statusWithData;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected Class<? extends LongTaskRunner<?, ?, ?, Integer, ?, AbstractLockedResource<Subscription, Integer>>> getLongTaskRunnerClass() {
		return (Class) LongTaskRunnerSubscription.class;
	}

}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.*;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.task.LongTaskNodeRepository;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.resource.OnNullReturn404;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import java.util.*;
import java.util.function.BiFunction;

/**
 * {@link Node} resource.
 */
@Path("/node")
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class NodeResource extends AbstractLockedResource<Node, String> {

	@Autowired
	protected NodeRepository repository;

	@Autowired
	private EventResource eventResource;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private ParameterValueResource pvResource;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private PaginationJson paginationJson;

	@Autowired
	protected NodeResource self;

	/**
	 * Mapped columns.
	 */
	private static final Map<String, String> ORM_MAPPING = new HashMap<>();

	static {
		ORM_MAPPING.put("name", "name");
	}

	/**
	 * Daily, Check status of each node instance.
	 */
	@Scheduled(cron = "${health.node}")
	public void checkNodesStatusScheduler() {
		checkNodesStatus(repository.findAllInstance());
	}

	/**
	 * Create a new {@link Node}.
	 *
	 * @param vo The new node definition.
	 */
	@POST
	@CacheRemoveAll(cacheName = "nodes")
	public void create(final NodeEditionVo vo) {
		final var entity = new Node();

		// Also check the parent is writable
		if (vo.isRefining()) {
			checkWritableNode(vo.getRefined());
		}

		saveOrUpdate(vo, entity);

		// Create and the new parameters
		pvResource.create(vo.getParameters(), entity);
	}

	/**
	 * Update an existing {@link Node}.
	 *
	 * @param vo The new node definition to replace.
	 */
	@PUT
	@CacheRemoveAll(cacheName = "nodes")
	public void update(final NodeEditionVo vo) {
		final var entity = saveOrUpdate(vo, checkWritableNode(vo.getId()));

		// Update parameters as needed
		if (!vo.isUntouchedParameters()) {
			// Create/update/delete the parameters
			pvResource.update(vo.getParameters(), entity);
		}
	}

	private Node saveOrUpdate(final NodeEditionVo vo, final Node entity) {
		NamedBean.copy(vo, entity);
		entity.setRefined(checkRefined(vo));
		entity.setMode(checkMode(vo, entity));

		// Check the parameters : data and structure
		checkInputParameters(vo).forEach(p -> pvResource.checkOwnership(p, entity));
		repository.saveAndFlush(entity);
		return entity;
	}

	/**
	 * Check the desired mode is a subset of allowed modes.
	 */
	private SubscriptionMode checkMode(final NodeEditionVo vo, final Node entity) {
		if (entity.isRefining()) {
			// Check the mode inheritance
			return checkMode(entity.getRefined().getMode(), vo.getMode());
		}
		return vo.getMode();
	}

	/**
	 * Check the desired mode is a subset of allowed modes. Accepted couples (parent, node) are :
	 * <ul>
	 * <li>ALL,*</li>
	 * <li>X,X</li>
	 * <li>*,NONE</li>
	 * </ul>
	 */
	private SubscriptionMode checkMode(final SubscriptionMode parentMode, final SubscriptionMode nodeMode) {
		if (nodeMode == SubscriptionMode.NONE || parentMode == nodeMode || parentMode == SubscriptionMode.ALL) {
			// Checked node's mode
			return nodeMode;
		}
		// Node's mode overflow the refined one
		throw new ValidationJsonException("mode", "invalid-mode", "mode", nodeMode, "refined", parentMode);
	}

	/**
	 * Check the desired refined and the naming convention. Return the resolved and validated refined of
	 * <code>null</code> when is root.
	 */
	private Node checkRefined(final NodeEditionVo node) {
		if (node.isRefining()) {
			final var parent = node.getRefined();
			// Check this parent is the direct ancestor
			if (!node.getId().matches(parent + ":[^:]+")) {
				// Parent is in a different branch, or invalid depth
				throw new ValidationJsonException("refined", "invalid-parent", "id", node.getId(), "refined", parent);
			}
			// Check the refined node is existing
			return repository.findOneExpected(parent);
		}
		// Check the current node can be a root node, AKA a service.
		if (!node.getId().matches("service:[^:]+")) {
			// Identifier does not match to a root
			throw new ValidationJsonException("refined", "invalid-parent", node.getId());
		}

		// A true root node
		return null;
	}

	/**
	 * Delete an existing {@link Node} from its identifier. The whole cache of nodes is invalidated. The deletion can
	 * only succeed if there are no related subscription. They need to be previously deleted. The administrator rights
	 * are also checked.
	 *
	 * @param id The node identifier.
	 * @throws Exception When the related plug-in implementation throws an exception during the deletion.
	 */
	@DELETE
	@Path("{id:service:.+:.+:.*}")
	@CacheRemoveAll(cacheName = "nodes")
	public void delete(@PathParam("id") final String id) throws Exception {
		checkAdministrable(id);
		final var nbSubscriptions = subscriptionRepository.countByNode(id);
		if (nbSubscriptions > 0) {
			// Subscriptions need to be deleted first
			throw new BusinessException("existing-subscriptions", nbSubscriptions);
		}
		// Delegate the deletion
		deleteWithTasks(id, id, true);

		pvResource.deleteByNode(id);
		parameterRepository.deleteByNode(id);
		eventRepository.deleteByNode(id);
		repository.deleteById(id);
	}

	@Override
	protected void delete(final ServicePlugin plugin, final String id, final boolean deleteRemoteData)
			throws Exception {
		plugin.delete(id, deleteRemoteData);
	}

	/**
	 * Check status of each node instance. Only visible nodes from the current user are checked.
	 */
	@POST
	@Path("status/refresh")
	public void checkNodesStatus() {
		checkNodesStatus(repository.findAllInstance(securityHelper.getLogin()));
	}

	/**
	 * Check status of each node.
	 *
	 * @param nodes The nodes to check.
	 */
	private void checkNodesStatus(final List<Node> nodes) {
		nodes.forEach(this::checkNodeStatus);
	}

	/**
	 * Check status of a specific node instance. Only visible node from the current user is checked.
	 *
	 * @param id The node identifier to check.
	 * @return the new status or <code>null</code> if undefined of not visible.
	 */
	@POST
	@Path("status/refresh/{id:.+:.*}")
	@OnNullReturn404
	public NodeStatus checkNodeStatus(@PathParam("id") final String id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin())).map(this::checkNodeStatus)
				.orElse(null);
	}

	/**
	 * Check the status of a node.
	 *
	 * @param node The node to check.
	 * @return the new status.
	 */
	private NodeStatus checkNodeStatus(final Node node) {
		final var parameters = pvResource.getNodeParameters(node.getId());
		final var status = self.checkNodeStatus(node.getId(), parameters);
		if (eventResource.registerEvent(node, EventType.STATUS, status.name())) {
			checkSubscriptionStatus(node, status);
		}
		return status;
	}

	/**
	 * Check the status of a node. This method need to be public for the CGLIB proxying.
	 *
	 * @param node       The node identifier.
	 * @param parameters Node parameters used to check the status.
	 * @return The node status.
	 */
	public NodeStatus checkNodeStatus(final String node, final Map<String, String> parameters) {
		var isUp = false;
		log.info("Check status of node {}", node);
		try {
			// Find the plug-in associated to the requested node
			isUp = locator.getResourceExpected(node, ToolPlugin.class).checkStatus(node, parameters);
		} catch (final Exception e) { // NOSONAR
			// Do not pollute logs with this failures
			// Service is down when an exception is thrown.
			log.warn("Check status of node {} failed with {}: {}", node, e.getClass(), e.getMessage());
		}
		return NodeStatus.getValue(isUp);
	}

	/**
	 * Daily, check status of each subscription.
	 */
	@Scheduled(cron = "${health.subscription}")
	public void checkSubscriptionsStatusScheduler() {
		checkSubscriptionsStatus(repository.findAllInstance());
	}

	/**
	 * Check status of each subscription. Only visible nodes from the current user are checked.
	 */
	@POST
	@Path("status/subscription/refresh")
	public void checkSubscriptionsStatus() {
		checkSubscriptionsStatus(repository.findAllInstance(securityHelper.getLogin()));
	}

	/**
	 * Check the subscriptions of given nodes. The node may be checked if unknown.
	 *
	 * @param instances The nodes to check.
	 */
	private void checkSubscriptionsStatus(final List<Node> instances) {
		var counter = 0;
		log.info("Check all subscriptions of {} nodes : Started", instances.size());
		for (final var node : instances) {
			checkSubscriptionStatus(node, null);
			counter++;
			log.info("Check all subscriptions {}/{} processed nodes", counter, instances.size());
		}
		log.info("Check all subscriptions of {} nodes : Done", instances.size());
	}

	/**
	 * Find subscriptions where some parameters are defined.
	 *
	 * @param id node identifier
	 * @return subscriptions with redefined parameters
	 */
	protected Map<Subscription, Map<String, String>> findSubscriptionsWithParams(final String id) {
		final var result = new HashMap<Subscription, Map<String, String>>();
		for (final var entityTab : subscriptionRepository.findAllWithValuesByNode(id)) {
			final var value = (ParameterValue) entityTab[1];
			result.computeIfAbsent((Subscription) entityTab[0], s -> new HashMap<>()).put(value.getParameter().getId(),
					value.getData());
		}
		return result;
	}

	/**
	 * Check status subscription.
	 *
	 * @param node   node where we must check subscriptions
	 * @param status node status
	 */
	protected void checkSubscriptionStatus(final Node node, final NodeStatus status) {
		final var nodeParameters = pvResource.getNodeParameters(node.getId());

		// Retrieve subscriptions where parameters are redefined.
		// Other subscriptions get the node's status.
		final var subscriptions = findSubscriptionsWithParams(node.getId());

		var newStatus = status;
		if (status == null) {
			// Node status is unknown for now, need a check
			newStatus = NodeStatus.getValue(self.checkNodeStatus(node.getId(), nodeParameters).isUp());

			// Update the node status
			eventResource.registerEvent(node, EventType.STATUS, newStatus.name());
		}

		// Check the subscriptions
		if (newStatus.isUp()) {
			// Check only the subscription in UP nodes
			checkNodeSubscriptions(node, nodeParameters, subscriptions);
		} else {
			// All subscription of this are marked as DOWN
			log.info("Node {} is DOWN, as well for {} related subscriptions", node.getId(), subscriptions.size());
			subscriptions.forEach((s, ignored) -> eventResource.registerEvent(s, EventType.STATUS, NodeStatus.DOWN.name()));
		}
	}

	/**
	 * Check status for a subscription.
	 *
	 * @param subscription Subscription entity.
	 * @param parameters   Parameters of a subscription.
	 * @return status of given subscription.
	 */
	public SubscriptionStatusWithData checkSubscriptionStatus(final Subscription subscription,
			final Map<String, String> parameters) {
		final var node = subscription.getNode().getId();
		try {
			log.info("Check status of subscription {}#{}...", node, subscription.getId());

			// Find the plug-in associated to the requested node
			final var toolPlugin = locator.getResourceExpected(node, ToolPlugin.class);

			// Call service which check status
			final var status = toolPlugin.checkSubscriptionStatus(subscription.getId(), node, parameters);
			status.setNode(node);
			log.info("Check status of subscription {}#{} succeed", node, subscription.getId());
			return status;
		} catch (final Exception e) { // NOSONAR
			// Do not pollute logs with this failures
			// Service is down when an exception is thrown, log the error
			// without trace
			log.warn("Check status of subscription {}#{} failed : {}", node, subscription.getId(), e.getMessage());
		}
		return new SubscriptionStatusWithData(false);
	}

	/**
	 * Check the subscriptions of each subscription related to given node.
	 */
	private void checkNodeSubscriptions(final Node node, final Map<String, String> nodeParameters,
			final Map<Subscription, Map<String, String>> subscriptions) {
		var counter = 0;
		for (final var subscription : subscriptions.entrySet()) {
			// For each subscription, check status
			log.info("Check all subscriptions of node {} : {}/{} ...", node.getId(), counter + 1, subscriptions.size());
			final var parameters = new HashMap<>(nodeParameters);
			parameters.putAll(subscription.getValue());
			final var subscriptionStatus = self.checkSubscriptionStatus(subscription.getKey(), parameters).getStatus();
			eventResource.registerEvent(subscription.getKey(), EventType.STATUS, subscriptionStatus.name());
			counter++;
		}
	}

	/**
	 * Retrieve nodes status.
	 *
	 * @return Status statistics of all nodes.
	 */
	@GET
	@Path("status")
	public List<EventVo> getNodeStatus() {
		return eventResource.findAll(securityHelper.getLogin());
	}

	/**
	 * Retrieve a specific node status.
	 *
	 * @param id The node to check.
	 * @return Status of a single node. Many be <code>null</code> when node is not found or when there is unknown
	 * status.
	 */
	@GET
	@Path("status/{id:.+:.*}")
	@OnNullReturn404
	public NodeStatus getNodeStatus(@PathParam("id") final String id) {
		return Optional.ofNullable(eventResource.findByNode(securityHelper.getLogin(), id)).map(EventVo::getValue)
				.map(v -> EnumUtils.getEnum(NodeStatus.class, v)).orElse(null);
	}

	/**
	 * Retrieve node statistics.
	 *
	 * @return Last known status of all nodes.
	 */
	@GET
	@Path("status/subscription")
	public List<NodeStatisticsVo> getNodeStatistics() {
		final var results = new HashMap<String, NodeStatisticsVo>();
		final var subscriptionsSpecificEvents = eventRepository.countSubscriptionsEvents(securityHelper.getLogin());
		final var totalSubscriptions = repository.countNodeSubscriptions(securityHelper.getLogin());

		// Map node and amount of subscriptions
		for (final var totalSubscription : totalSubscriptions) {
			final var result = new NodeStatisticsVo((String) totalSubscription[0]);
			result.getValues().put("total", (Long) totalSubscription[1]);
			results.put(result.getNode(), result);
		}

		// Map status of each subscription
		for (final var subscriptionsSpecificEvent : subscriptionsSpecificEvents) {
			final var result = results.computeIfAbsent((String) subscriptionsSpecificEvent[0], NodeStatisticsVo::new);
			result.getValues().put((String) subscriptionsSpecificEvent[1], (Long) subscriptionsSpecificEvent[2]);
		}

		return new ArrayList<>(results.values());
	}

	/**
	 * Return a specific node visible for current user. The visibility is checked.
	 *
	 * @param id The node identifier.
	 * @return The visible node. Never <code>null</code>.
	 */
	@GET
	@Path("{id:.+:.*}")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeVo findById(@PathParam("id") final String id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin()))
				.map(n -> NodeHelper.toVoLight(n, locator)).orElseThrow(() -> new ValidationJsonException("id",
						BusinessException.KEY_UNKNOWN_ID, "0", "node", "1", id));
	}

	/**
	 * Return a specific node details. The visibility is not checked, and the cache is not involved.
	 *
	 * @param id The node identifier.
	 * @return The node. Cannot be <code>null</code>.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeVo findByIdInternal(final String id) {
		return NodeHelper.toVo(repository.findOneExpected(id), locator);
	}

	/**
	 * Return all visible nodes for current user. The hierarchy data is returned but without UI data.
	 *
	 * @param uriInfo  pagination data.
	 * @param criteria the optional criteria to match.
	 * @param refined  The optional parent identifier to be like. Special attention for 'service' value corresponding to
	 *                 the root.
	 * @param mode     Expected subscription mode. When <code>null</code>, the node's mode is not checked.
	 * @param depth    The maximal depth. When <code>0</code> means no refined, so basically services only.
	 *                 <code>1</code> means refined is a service, so nodes are basically tool only. <code>2</code> means
	 *                 refined is a tool, so nodes are basically instances only. For the other cases, there is no limit,
	 *                 and corresponds to the default behavior.
	 * @return All visible nodes with the hierarchy but without UI data.
	 */
	@GET
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public TableItem<NodeVo> findAll(@Context final UriInfo uriInfo,
			@QueryParam(DataTableAttributes.SEARCH) final String criteria, @QueryParam("refined") final String refined,
			@QueryParam("mode") final SubscriptionMode mode, @QueryParam("depth") @DefaultValue("-1") final int depth) {
		final var findAll = repository.findAllVisible(securityHelper.getLogin(), StringUtils.trimToEmpty(criteria),
				refined, mode, depth, paginationJson.getPageRequest(uriInfo, ORM_MAPPING));

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, n -> NodeHelper.toVo(n, locator));
	}

	/**
	 * Return all nodes with the hierarchy. Key is the identifier of the nodes.
	 *
	 * @return all nodes without UI data.
	 */
	@CacheResult(cacheName = "nodes")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Map<String, NodeVo> findAll() {
		return NodeHelper.toVoParameters(repository.findAllWithValuesSecure(), locator);
	}

	/**
	 * Check the parameters that are being attached to this node : overrides, mandatory and ownerships.
	 *
	 * @param vo The parameterized object.
	 * @return The corresponding and also validated {@link Parameter} entities.
	 */
	public List<Parameter> checkInputParameters(final AbstractParameterizedVo vo) {
		final var acceptedParameters = parameterRepository.getOrphanParameters(vo.getNode(), vo.getMode(),
				securityHelper.getLogin());

		// Check all mandatory parameters for the current subscription mode
		vo.setParameters(ListUtils.emptyIfNull(vo.getParameters()));

		// Check there is no override
		checkOverrides(acceptedParameters.stream().map(Parameter::getId).toList(),
				vo.getParameters().stream().map(ParameterValueCreateVo::getParameter).toList());
		return acceptedParameters;
	}

	/**
	 * Check the given parameters do not override a valued parameter.
	 */
	private void checkOverrides(final List<String> acceptedParameters, final List<String> parameters) {
		final var overrides = CollectionUtils.removeAll(parameters, acceptedParameters);
		if (!overrides.isEmpty()) {
			// A non acceptable parameter. An attempt to override a secured data?
			throw ValidationJsonException.newValidationJsonException("not-accepted-parameter",
					overrides.iterator().next());
		}
	}

	/**
	 * Check the related node can be updated by the current principal.
	 *
	 * @param id      The node identifier to check.
	 * @param checker The function checking the node from its identifier (first parameter) for a given user (second
	 *                parameter) and return the check {@link Node} identity.
	 * @return the checked node.
	 */
	public Node checkNode(final String id, final BiFunction<String, String, Node> checker) {
		final var node = checker.apply(id, securityHelper.getLogin());
		if (node == null) {
			// Node is not readable or does not exist
			throw new BusinessException("read-only-node", "node", id);
		}
		return node;
	}

	/**
	 * Check the related node can be updated by the current principal.
	 *
	 * @param id The node identifier to check.
	 * @return The node the principal can write.
	 */
	public Node checkWritableNode(final String id) {
		return checkNode(id, repository::findOneWritable);
	}

	/**
	 * Check the related node can be deleted by the current principal.
	 *
	 * @param id The node identifier to check.
	 * @return The node the principal can administer.
	 */
	public Node checkAdministrable(final String id) {
		return checkNode(id, repository::findOneAdministrable);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected Class<? extends LongTaskRunner<AbstractLongTaskNode, LongTaskNodeRepository<AbstractLongTaskNode>, Node, String, NodeRepository, AbstractLockedResource<Node, String>>> getLongTaskRunnerClass() {
		return (Class) LongTaskRunnerNode.class;
	}

	@Override
	public Node checkVisible(String id) {
		final var entity = repository.findOneVisible(id, securityHelper.getLogin());
		if (entity == null) {
			// Associated node is not visible
			throw new EntityNotFoundException(id);
		}
		return entity;
	}
}

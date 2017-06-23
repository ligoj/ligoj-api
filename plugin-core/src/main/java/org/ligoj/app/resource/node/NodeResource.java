package org.ligoj.app.resource.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.transaction.Transactional;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.CacheManager;

/**
 * {@link Node} resource.
 */
@Path("/node")
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class NodeResource {

	@Autowired
	private NodeRepository repository;

	@Autowired
	private EventResource eventResource;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	protected ServicePluginLocator servicePluginLocator;

	@Autowired
	private ParameterValueResource parameterValueResource;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private PaginationJson paginationJson;

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORM_MAPPING = new HashMap<>();

	static {
		ORM_MAPPING.put("name", "name");
	}

	/**
	 * {@link Node} JPA to business object transformer.
	 * 
	 * @param entity
	 *            Source entity.
	 * @return The corresponding VO object with recursive redefined reference.
	 */
	public static NodeVo toVo(final Node entity) {
		final NodeVo vo = toVoLight(entity);
		if (entity.isRefining()) {
			vo.setRefined(toVo(entity.getRefined()));
		}
		return vo;
	}

	/**
	 * {@link Node} JPA to business object transformer with parameters.
	 * 
	 * @param entity
	 *            Source entity.
	 * @return The corresponding VO object with resources and without recursive
	 *         parent reference.
	 */
	private static NodeVo toVoParameter(final Node entity) {
		final NodeVo vo = toVoLight(entity);
		vo.setParameters(new HashMap<>());
		vo.setTag(entity.getTag());
		vo.setTagUiClasses(entity.getTagUiClasses());
		return vo;
	}

	/**
	 * {@link Node} JPA to VO object transformer without refined informations.
	 * 
	 * @param entity
	 *            Source entity.
	 * @return The corresponding VO object without recursive redefined
	 *         reference.
	 */
	public static NodeVo toVoLight(final Node entity) {
		final NodeVo vo = new NodeVo();
		NamedBean.copy(entity, vo);
		vo.setMode(entity.getMode());
		vo.setUiClasses(entity.getUiClasses());
		return vo;
	}

	/**
	 * JPA {@link Node} associated to {@link ParameterValue} to detailed
	 * {@link NodeVo} converter. This not a one to one {@link Function}.
	 * 
	 * @param nodesAndValues
	 *            Nodes with values.
	 * @return The corresponding VO objects with recursive redefined reference.
	 */
	public static Map<String, NodeVo> toVoParameters(final List<Object[]> nodesAndValues) {

		// Build the nodes
		final Map<String, NodeVo> nodes = new HashMap<>();
		final Map<String, Node> entities = new HashMap<>();
		for (final Object[] resultSet : nodesAndValues) {
			final Node node = (Node) resultSet[0];
			final NodeVo vo = nodes.computeIfAbsent(node.getId(), id -> {
				// Build the first encountered parameter for this node
				entities.put(id, node);
				return toVoParameter(node);
			});

			// Copy the parameter value if present
			Optional.ofNullable((ParameterValue) resultSet[1]).ifPresent(
					v -> vo.getParameters().put(v.getParameter().getId(), ParameterValueResource.parseValue(v, new ParameterValueVo())));
		}

		// Complete the hierarchy
		entities.entrySet().stream().filter(entry -> entry.getValue().isRefining()).forEach(entry -> {
			// Complete the hierarchy for this node
			final NodeVo node = nodes.get(entry.getKey());
			final NodeVo parent = nodes.get(entry.getValue().getRefined().getId());
			node.setRefined(parent);
			node.getParameters().putAll(parent.getParameters());
		});
		return nodes;
	}

	/**
	 * Return all parameters definition where a value is expected to be attached
	 * to the final subscription in case of linking a project to an existing or
	 * a new instance inside the provided node.
	 * 
	 * @param id
	 *            The node identifier.
	 * @param mode
	 *            Subscription mode.
	 * @return All parameter definitions where a value is expected to be
	 *         attached to the final subscription in given mode.
	 */
	@GET
	@Path("{id:.+:.*}/parameter/{mode}")
	public List<ParameterVo> getNotProvidedParameters(@PathParam("id") final String id, @PathParam("mode") final SubscriptionMode mode) {
		return repository.getOrphanParameters(id, mode, securityHelper.getLogin()).stream().map(ParameterResource::toVo)
				.collect(Collectors.toList());
	}

	/**
	 * Return the parameters of given node. Not exposed as web-service since
	 * secured data are clearly exposed. The result is cached.
	 * 
	 * @param node
	 *            the node identifier.
	 * @return the parameters of given node as {@link Map}.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	@CacheResult(cacheName = "node-parameters")
	public Map<String, String> getParametersAsMap(@CacheKey final String node) {
		// Get parameters of given node
		return parameterValueResource.toMapValues(repository.getParameterValues(node));
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
	 * @param node
	 *            The new node definition.
	 */
	@POST
	@CacheRemoveAll(cacheName = "nodes")
	public void create(final NodeEditionVo node) {
		saveOrUpdate(node, new Node());
	}

	/**
	 * Update an existing {@link Node}.
	 * 
	 * @param node
	 *            The new node definition to replace.
	 */
	@PUT
	@CacheRemoveAll(cacheName = "nodes")
	public void update(final NodeEditionVo node) {
		saveOrUpdate(node, repository.findOneExpected(node.getId()));
	}

	private void saveOrUpdate(final NodeEditionVo node, final Node entity) {
		NamedBean.copy(node, entity);
		if (node.isRefining()) {
			final String parent = node.getRefined();
			// Check this parent is the direct ancestor
			if (!node.getId().matches(parent + ":\\w+")) {
				// Parent is in a different branch, or invalid depth
				throw new ValidationJsonException("refined", "invalid-parent", "id", node.getId(), "refined", parent);
			}
			// Check the refined node is existing
			entity.setRefined(repository.findOneExpected(parent));
		} else {
			// Check the current node can be a root node, AKA a service.
			if (!node.getId().matches("service:\\w+")) {
				// Identifier does not match to a root
				throw new ValidationJsonException("refined", "invalid-parent", node.getId());
			}
			entity.setRefined(null);
		}
		entity.setMode(node.getMode());
		repository.saveAndFlush(entity);
	}

	/**
	 * Delete an existing {@link Node} from its identifier. The whole cache of
	 * nodes is invalidated. All related subscriptions are also deleted.
	 * 
	 * @param id
	 *            The node identifier.
	 */
	@DELETE
	@Path("{id:service:.+:.+:.*}")
	@CacheRemoveAll(cacheName = "nodes")
	public void delete(@PathParam("id") final String id) {
		parameterValueRepository.deleteByNode(id);
		parameterRepository.deleteByNode(id);
		eventRepository.deleteByNode(id);
		subscriptionRepository.deleteAllBy("node.id", id);
		repository.delete(id);

		// Also invalidates the node parameters cache
		// Note "subscription-parameters" cache is not invalidated. A process
		// could be added there to invalidate each related subscription.
		CacheManager.getInstance().removeCache("node-parameters");
	}

	/**
	 * Check status of each node instance. Only visible nodes from the current
	 * user are checked.
	 */
	@POST
	@Path("status/refresh")
	public void checkNodesStatus() {
		checkNodesStatus(repository.findAllInstance(securityHelper.getLogin()));
	}

	/**
	 * Check status of a specific node instance. Only visible node from the
	 * current user is checked.
	 * 
	 * @param id
	 *            The node identifier to check.
	 */
	@POST
	@Path("status/refresh/{id:.+:.*}")
	public void checkNodeStatus(@PathParam("id") final String id) {
		Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin())).ifPresent(this::checkNodeStatus);
	}

	/**
	 * Check status of each node.
	 * 
	 * @param nodes
	 *            The nodes to check.
	 */
	private void checkNodesStatus(final List<Node> nodes) {
		nodes.forEach(this::checkNodeStatus);
	}

	/**
	 * Check the status of a node.
	 * 
	 * @param node
	 *            The node to check.
	 */
	private void checkNodeStatus(final Node node) {
		final Map<String, String> parameters = getParametersAsMap(node.getId());
		final NodeStatus status = SpringUtils.getBean(NodeResource.class).checkNodeStatus(node.getId(), parameters);
		if (eventResource.registerEvent(node, EventType.STATUS, status.name())) {
			checkSubscriptionStatus(node, status);
		}
	}

	/**
	 * Check the status of a node.
	 * 
	 * @param node
	 *            The node identifier.
	 * @param parameters
	 *            Node parameters used to check the status.
	 * @return The node status.
	 */
	public NodeStatus checkNodeStatus(final String node, final Map<String, String> parameters) {
		boolean isUp = false;
		log.info("Check status of node {}", node);
		try {
			// Find the plug-in associated to the requested node
			final ToolPlugin plugin = servicePluginLocator.getResourceExpected(node, ToolPlugin.class);

			// Call service which check status
			isUp = plugin.checkStatus(node, parameters);
		} catch (final Exception e) { // NOSONAR - Do not pollute logs with this
										// failures
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
	 * Check status of each subscription. Only visible nodes from the current
	 * user are checked.
	 */
	@POST
	@Path("status/subscription/refresh")
	public void checkSubscriptionsStatus() {
		checkSubscriptionsStatus(repository.findAllInstance(securityHelper.getLogin()));
	}

	/**
	 * Check the subscriptions of given nodes. The node my be checked if
	 * unknown.
	 * 
	 * @param instances
	 *            The nodes to check.
	 */
	private void checkSubscriptionsStatus(final List<Node> instances) {
		int counter = 0;
		log.info("Check all subscriptions of {} nodes : Started", instances.size());
		for (final Node node : instances) {
			checkSubscriptionStatus(node, null);
			counter++;
			log.info("Check all subscriptions {}/{} processed nodes", counter, instances.size());
		}
		log.info("Check all subscriptions of {} nodes : Done", instances.size());
	}

	/**
	 * find subscriptions where some parameters defined.
	 * 
	 * @param key
	 *            node key
	 * @return subscriptions with redefined parameters
	 */
	protected Map<Subscription, Map<String, String>> findSubscriptionsWithParams(final String key) {
		final Map<Subscription, Map<String, String>> result = new HashMap<>();
		for (final Object[] entityTab : subscriptionRepository.findAllWithValuesByNode(key)) {
			final ParameterValue value = (ParameterValue) entityTab[1];
			result.computeIfAbsent((Subscription) entityTab[0], s -> new HashMap<>()).put(value.getParameter().getId(), value.getData());
		}
		return result;
	}

	/**
	 * Check status subscription.
	 * 
	 * @param node
	 *            node where we must check subscriptions
	 * @param status
	 *            node status
	 */
	public void checkSubscriptionStatus(final Node node, final NodeStatus status) {
		final Map<String, String> nodeParameters = getParametersAsMap(node.getId());

		// Retrieve subscriptions where parameters are redefined. Other
		// subscriptions have node
		// status.
		final Map<Subscription, Map<String, String>> subscriptionsToCheck = findSubscriptionsWithParams(node.getId());

		// Same instance, but with proxy to resolve inner transaction issue
		final NodeResource thisProxy = SpringUtils.getBean(NodeResource.class);

		NodeStatus newStatus = status;
		if (status == null) {
			// Node status is unknown for now, need a check
			newStatus = NodeStatus.getValue(thisProxy.checkNodeStatus(node.getId(), nodeParameters).isUp());

			// Update the node status
			eventResource.registerEvent(node, EventType.STATUS, newStatus.name());
		}

		// Check the subscriptions
		if (newStatus.isUp()) {
			// Check only the subscription in UP nodes
			int counter = 0;
			for (final Entry<Subscription, Map<String, String>> subscription : subscriptionsToCheck.entrySet()) {
				// For each subscription, check status
				log.info("Check all subscriptions of node {} : {}/{} ...", node.getId(), counter + 1, subscriptionsToCheck.size());
				final Map<String, String> parameters = new HashMap<>(nodeParameters);
				parameters.putAll(subscription.getValue());
				final NodeStatus subscriptionStatus = thisProxy.checkSubscriptionStatus(subscription.getKey(), parameters).getStatus();
				eventResource.registerEvent(subscription.getKey(), EventType.STATUS, subscriptionStatus.name());
				counter++;
			}
		} else {
			// All subscription of this are marked as DOWN
			log.info("Node {} is DOWN, as well for {} related subscriptions", node.getId(), subscriptionsToCheck.size());
			subscriptionsToCheck.entrySet().forEach(s -> eventResource.registerEvent(s.getKey(), EventType.STATUS, NodeStatus.DOWN.name()));
		}
	}

	/**
	 * Check status for a subscription.
	 * 
	 * @param subscription
	 *            Subscription entity.
	 * @param parameters
	 *            Parameters of a subscription.
	 * @return status of given subscription.
	 */
	public SubscriptionStatusWithData checkSubscriptionStatus(final Subscription subscription, final Map<String, String> parameters) {
		final String node = subscription.getNode().getId();
		try {
			log.info("Check status of a subscription attached to {}...", node);

			// Find the plug-in associated to the requested node
			final ToolPlugin toolPlugin = servicePluginLocator.getResourceExpected(node, ToolPlugin.class);

			// Call service which check status
			final SubscriptionStatusWithData status = toolPlugin.checkSubscriptionStatus(subscription.getId(), node, parameters);
			status.setNode(node);
			log.info("Check status of a subscription attached to {} succeed", node);
			return status;
		} catch (final Exception e) { // NOSONAR - Do not pollute logs with this
										// failures
			// Service is down when an exception is thrown, log the error
			// without trace
			log.warn("Check status of a subscription attached to {} failed : {}", node, e.getMessage());
		}
		return new SubscriptionStatusWithData(false);
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
	 * Retrieve node statistics.
	 * 
	 * @return Last known status of all nodes.
	 */
	@GET
	@Path("status/subscription")
	public List<NodeStatisticsVo> getNodeStatistics() {
		final Map<String, NodeStatisticsVo> results = new HashMap<>();
		final List<Object[]> subscriptionsSpecificEvents = eventRepository.countSubscriptionsEvents(securityHelper.getLogin());
		final List<Object[]> totalSubscriptions = repository.countNodeSubscriptions(securityHelper.getLogin());

		// Map node and amount of subscriptions
		for (final Object[] totalSubscription : totalSubscriptions) {
			final NodeStatisticsVo result = new NodeStatisticsVo((String) totalSubscription[0]);
			result.getValues().put("total", (Long) totalSubscription[1]);
			results.put(result.getNode(), result);
		}

		// Map status of each subscription
		for (final Object[] subscriptionsSpecificEvent : subscriptionsSpecificEvents) {
			final NodeStatisticsVo result = results.computeIfAbsent((String) subscriptionsSpecificEvent[0], NodeStatisticsVo::new);
			result.getValues().put((String) subscriptionsSpecificEvent[1], (Long) subscriptionsSpecificEvent[2]);
		}

		return new ArrayList<>(results.values());
	}

	/**
	 * Return a specific node visible for current user. The visibility is
	 * checked.
	 * 
	 * @param id
	 *            The node identifier.
	 * @return The visible node. Never <code>null</code>.
	 */
	@GET
	@Path("{id:.+:.*}")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeVo findById(@PathParam("id") final String id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin())).map(NodeResource::toVoLight)
				.orElseThrow(() -> new ValidationJsonException("id", BusinessException.KEY_UNKNOW_ID, "0", "node", "1", id));
	}

	/**
	 * Return a specific node details. The visibility is not checked, and the
	 * cache is not involved.
	 * 
	 * @param id
	 *            The node identifier.
	 * @return The node. Cannot be <code>null</code>.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeVo findByIdInternal(final String id) {
		return toVo(repository.findOneExpected(id));
	}

	/**
	 * Return all nodes with the hierarchy but without UI data.
	 * 
	 * @param uriInfo
	 *            pagination data.
	 * @param criteria
	 *            the optional criteria to match.
	 * @param parent
	 *            The optional parent identifier to be like. Special attention
	 *            for 'service' value corresponding to the root.
	 * @param mode
	 *            Expected subscription mode. When <code>null</code>, the node's
	 *            mode is not checked.
	 * @param depth
	 *            The maximal depth. When <code>0</code> means no refined, so
	 *            basically services only. <code>1</code> means refined is a
	 *            service, so nodes are basically tool only. <code>2</code>
	 *            means refined is a tool, so nodes are basically instances
	 *            only. For the other cases, there is no limit, and corresponds
	 *            to the default behavior.
	 * @return All nodes with the hierarchy but without UI data.
	 */
	@GET
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public TableItem<NodeVo> findAll(@Context final UriInfo uriInfo, @QueryParam(DataTableAttributes.SEARCH) final String criteria,
			@QueryParam("refined") final String refined, @QueryParam("mode") final SubscriptionMode mode,
			@QueryParam("depth") @DefaultValue("-1") final int depth) {
		final Page<Node> findAll = repository.findAllVisible(securityHelper.getLogin(), StringUtils.trimToNull(criteria), refined, mode,
				depth, paginationJson.getPageRequest(uriInfo, ORM_MAPPING));

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, NodeResource::toVo);
	}

	/**
	 * Return all nodes with the hierarchy. Key is the identifier of the nodes.
	 * 
	 * @return all nodes without UI data.
	 */
	@CacheResult(cacheName = "nodes")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Map<String, NodeVo> findAll() {
		return toVoParameters(repository.findAllWithValuesSecure());
	}
}

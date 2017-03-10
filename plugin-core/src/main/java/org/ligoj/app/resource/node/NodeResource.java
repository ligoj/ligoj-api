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
import javax.cache.annotation.CacheResult;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.ServicePluginLocator;
import lombok.extern.slf4j.Slf4j;

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
		if (entity.getRefined() != null) {
			vo.setRefined(toVo(entity.getRefined()));
		}
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
		DescribedBean.copy(entity, vo);
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
			NodeVo vo = nodes.get(node.getId());

			// Build the first encountered parameter for this node
			if (vo == null) {
				vo = new NodeVo();
				DescribedBean.copy(node, vo);
				vo.setParameters(new HashMap<>());
				vo.setTag(node.getTag());
				vo.setTagUiClasses(node.getTagUiClasses());
				vo.setUiClasses(node.getUiClasses());
				nodes.put(node.getId(), vo);
				entities.put(node.getId(), node);
			}

			// Add node value
			final ParameterValue parameterValue = (ParameterValue) resultSet[1];
			if (parameterValue != null) {
				// Copy the parameter value
				vo.getParameters().put(parameterValue.getParameter().getId(),
						ParameterValueResource.parseValue(parameterValue, new ParameterValueVo()));
			}
		}

		// Complete the hierarchy
		entities.entrySet().stream().filter(entry -> entry.getValue().getRefined() != null).forEach(entry -> {
			// Complete the hierarchy for this node
			final NodeVo node = nodes.get(entry.getKey());
			final NodeVo parent = nodes.get(entry.getValue().getRefined().getId());
			node.setRefined(parent);
			node.getParameters().putAll(parent.getParameters());
		});
		return nodes;
	}

	/**
	 * Return root nodes. Also named "services".
	 * 
	 * @return root nodes.
	 */
	@GET
	@Path("children")
	public List<NodeVo> findAllNoParent() {
		return findAllByParent(null);
	}

	/**
	 * Return nodes having as parent the given node..
	 * 
	 * @param id
	 *            The node identifier.
	 * @return tools implementing the given service.
	 */
	@GET
	@Path("{id}/children")
	public List<NodeVo> findAllByParent(@PathParam("id") final String id) {
		return findAllByParent(id, null);
	}

	/**
	 * Return nodes (light form) having as parent the given node.
	 * 
	 * @param id
	 *            The optional parent node identifier. When <code>null</code>,
	 *            root services are returned.
	 * @param mode
	 *            Optional Subscription mode.
	 * @return Nodes implementing the given service. Note this is a light form
	 *         of the node, without recursive redefinition.
	 */
	@GET
	@Path("{id}/children/{mode}")
	public List<NodeVo> findAllByParent(@PathParam("id") final String id, @PathParam("mode") final SubscriptionMode mode) {
		return repository.findAllByParent(id, mode, securityHelper.getLogin()).stream().map(NodeResource::toVoLight).collect(Collectors.toList());
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
	 * @return all parameters definition where a value is expected to be
	 *         attached to the final subscription in given mode.
	 */
	@GET
	@Path("{id}/parameter/{mode}")
	public List<ParameterVo> getNotProvidedParameters(@PathParam("id") final String id, @PathParam("mode") final SubscriptionMode mode) {
		return repository.getOrphanParameters(id, mode, securityHelper.getLogin()).stream().map(ParameterValueResource::toVo)
				.collect(Collectors.toList());
	}

	/**
	 * Return the parameters of given node. Not exposed as service since secured
	 * data are decrypted. The result is cached.
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
	@Path("status/refresh/{id}")
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
	 * Check status of a node.
	 * 
	 * @param node
	 *            The node identifier.
	 * @param parameters
	 *            Node parameters.
	 * @return status
	 */
	public NodeStatus checkNodeStatus(final String node, final Map<String, String> parameters) {
		boolean isUp;
		try {
			// Find the plug-in associated to the requested node
			final ToolPlugin plugin = servicePluginLocator.getResourceExpected(node, ToolPlugin.class);

			// Call service which check status
			isUp = plugin.checkStatus(node, parameters);
		} catch (final Exception e) { // NOSONAR - Do not pollute logs with this failures
			log.warn("Node {} is down : {}: {}", node, e.getClass(), e.getMessage());
			isUp = false; // service is down when an exception is thrown.
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

	private int checkSubscriptionsStatus(final List<Node> instances) {
		int counter = 0;
		for (final Node node : instances) {
			checkSubscriptionStatus(node, null);
			counter++;
			log.info("Check all statuses : {}/{}", counter, instances.size());
		}
		return counter;
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

		// retrieve subscriptions where parameters are redefined. Other
		// subscriptions have node status.
		final Map<Subscription, Map<String, String>> subscriptionsToCheck = findSubscriptionsWithParams(node.getId());

		// Same instance, but with proxy to resolve inner transaction issue
		final NodeResource thisProxy = SpringUtils.getBean(NodeResource.class);

		final NodeStatus newStatus;
		if (status == null) {
			// Node status is unknown for now...
			newStatus = NodeStatus.getValue(thisProxy.checkNodeStatus(node.getId(), nodeParameters).isUp());

			// Update the node status
			eventResource.registerEvent(node, EventType.STATUS, newStatus.name());
		} else {
			newStatus = status;
		}

		// Check the subscriptions
		int counter = 0;
		for (final Entry<Subscription, Map<String, String>> subscription : subscriptionsToCheck.entrySet()) {
			if (newStatus.isUp()) {
				// for each subscription, check status
				final Map<String, String> parameters = new HashMap<>(nodeParameters);
				parameters.putAll(subscription.getValue());
				eventResource.registerEvent(subscription.getKey(), EventType.STATUS,
						thisProxy.checkSubscriptionStatus(node.getId(), parameters).getStatus().name());
			} else {
				// node is down -> subscription is down too
				eventResource.registerEvent(subscription.getKey(), EventType.STATUS, NodeStatus.DOWN.name());
			}
			counter++;
			log.info("Check all statuses of node {} : {}/{}", node.getId(), counter, subscriptionsToCheck.size());

		}
	}

	/**
	 * Check status for a subscription.
	 * 
	 * @param node
	 *            Node identifier.
	 * @param parameters
	 *            Parameters of a subscription.
	 * @return status of given subscription.
	 */
	public SubscriptionStatusWithData checkSubscriptionStatus(final String node, final Map<String, String> parameters) {
		try {
			log.info("Check status of a subscription attached to {}...", node);

			// Find the plug-in associated to the requested node
			final ToolPlugin toolPlugin = servicePluginLocator.getResourceExpected(node, ToolPlugin.class);

			// Call service which check status
			final SubscriptionStatusWithData status = toolPlugin.checkSubscriptionStatus(parameters);
			status.setNode(node);
			log.info("Check status of a subscription attached to {} succeed", node);
			return status;
		} catch (final Exception e) { // NOSONAR - Do not pollute logs with this failures
			// Service is down when an exception is thrown, log the error without trace
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
		final List<Event> events = eventRepository.findLastEvents(securityHelper.getLogin());
		final Map<String, EventVo> services = new HashMap<>();
		final Map<String, EventVo> tools = new HashMap<>();
		for (final Event event : events) {
			final Node parent = event.getNode().getRefined();
			fillParentEvents(tools, parent, EventResource.toVo(event), event.getValue());
			fillParentEvents(services, parent.getRefined(), tools.get(parent.getId()), event.getValue());
		}
		return new ArrayList<>(services.values());
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

	private void fillParentEvents(final Map<String, EventVo> parents, final Node parent, final EventVo eventVo, final String eventValue) {
		final EventVo service = parents.computeIfAbsent(parent.getId(), key -> {
			final EventVo result = new EventVo();
			result.setNode(parent.getId());
			result.setLabel(parent.getName());
			result.setValue(eventValue);
			result.setType(eventVo.getType());
			return result;
		});
		service.getSpecifics().add(eventVo);
		if ("DOWN".equals(eventValue)) {
			service.setValue(eventValue);
		}
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
	public NodeVo findByIdExpected(@PathParam("id") final String id) {
		return Optional.ofNullable(findByIdVisible(id))
				.orElseThrow(() -> new ValidationJsonException("id", BusinessException.KEY_UNKNOW_ID, "0", "node", "1", id));
	}

	/**
	 * Return a specific node visible for current user. The visibility is
	 * checked.
	 * 
	 * @param id
	 *            The node identifier.
	 * @return The visible node. May be <code>null</code>.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeVo findByIdVisible(final String id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin())).map(NodeResource::toVoLight).orElse(null);
	}

	/**
	 * Return a specific node. The visibility is not checked.
	 * 
	 * @param id
	 *            The node identifier.
	 * @return The node. Cannot be <code>null</code>.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeVo findById(final String id) {
		return toVo(repository.findOneExpected(id));
	}

	/**
	 * Return all nodes with the hierarchy but without UI data.
	 * 
	 * @param uriInfo
	 *            pagination data.
	 * @param criteria
	 *            the optional criteria to match.
	 * @return All nodes with the hierarchy but without UI data.
	 */
	@GET
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public TableItem<NodeVo> findAll(@Context final UriInfo uriInfo, @QueryParam(DataTableAttributes.SEARCH) final String criteria) {
		final Page<Node> findAll = repository.findAllVisible(securityHelper.getLogin(), StringUtils.trimToNull(criteria),
				paginationJson.getPageRequest(uriInfo, ORM_MAPPING));

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

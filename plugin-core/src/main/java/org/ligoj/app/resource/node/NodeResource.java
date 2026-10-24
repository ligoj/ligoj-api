/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.ligoj.app.dao.task.LongTaskRepository;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.resource.OnNullReturn404;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import java.time.Instant;
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
	 * Cache of the locks of the scheduled health checks, shared by the cluster.
	 */
	public static final String HEALTH_LOCK_CACHE = "node-health-lock";

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CredentialExpiryPolicy credentialExpiryPolicy;

	@PersistenceContext
	private EntityManager em;

	/**
	 * Mapped columns.
	 */
	private static final Map<String, String> ORM_MAPPING = new HashMap<>();

	static {
		ORM_MAPPING.put("name", "name");
	}

	/**
	 * Periodically, according to the <code>health.node</code> cron, check status of each node instance. In a cluster, only one
	 * member runs it: the run is skipped while another member holds the lock, see {@link #HEALTH_LOCK_CACHE}.
	 */
	@Scheduled(cron = "${health.node}")
	public void checkNodesStatusScheduler() {
		runLocked("health.node", () -> checkNodesStatus(repository.findAllInstance()));
	}

	/**
	 * Run the given scheduled task when no other cluster member is running it. The lock is an entry of the
	 * {@link #HEALTH_LOCK_CACHE} cache, released at the end of the task. It also expires with the cache entry, after one
	 * hour by default (<code>cache.node-health-lock.ttl</code>): should the member holding it stop during the task, but
	 * also when the task runs longer, and then another member may start the same task while the first one still runs.
	 *
	 * @param name The task name, the lock key.
	 * @param task The task to run.
	 */
	@SuppressWarnings("unchecked")
	private void runLocked(final String name, final Runnable task) {
		final var lock = (javax.cache.Cache<Object, Object>) Objects.requireNonNull(cacheManager.getCache(HEALTH_LOCK_CACHE))
				.getNativeCache();
		if (!lock.putIfAbsent(name, Instant.now().toString())) {
			log.info("Scheduled task {} is already running on another cluster member, skipped", name);
			return;
		}
		try {
			task.run();
		} finally {
			lock.remove(name);
		}
	}

	/**
	 * Release the managed entities after the check of a node, when a transaction spans all the checks, as for a refresh
	 * request: the persistence context does not grow with the number of nodes, so the dirty checking of each flush stays
	 * bounded. The scheduled checks run without such a transaction, the scheduler invoking this bean itself, not its
	 * transactional proxy: each call then manages and releases its own entities, and a flush would be refused.
	 */
	private void releaseEntities() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			em.flush();
			em.clear();
		}
	}

	/**
	 * Create a new {@link Node}.
	 *
	 * @param vo The new node definition.
	 */
	@POST
	@CacheRemoveAll(cacheName = "nodes")
	public void create(final NodeEditionVo vo) {
		// A creation never overwrites an existing node
		if (repository.existsById(vo.getId())) {
			throw new ValidationJsonException("id", "already-exist", "0", "id", "1", vo.getId());
		}
		final var entity = new Node();

		// Check the parent is writable, or the principal is an administrator for a root node
		if (vo.isRefining()) {
			checkWritableNode(vo.getRefined());
		} else if (!securityHelper.isAdmin()) {
			throw new BusinessException("read-only-node", "node", vo.getId());
		}

		saveOrUpdate(vo, entity);

		// Create and the new parameters
		pvResource.create(vo.getParameters(), entity);
	}

	/**
	 * Update an existing {@link Node}. Its mode is kept, or widened from LINK or CREATE to ALL: the subscriptions
	 * created on the node keep being served.
	 *
	 * @param vo The new node definition to replace.
	 */
	@PUT
	@CacheRemoveAll(cacheName = "nodes")
	public void update(final NodeEditionVo vo) {
		final var entity = checkWritableNode(vo.getId());
		checkModeTransition(entity.getMode(), vo.getMode());
		saveOrUpdate(vo, entity);

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
		applyManualCredentialExpiry(vo, entity);

		// Check the parameters : data and structure
		checkInputParameters(vo).forEach(p -> pvResource.checkOwnership(p, entity));
		repository.saveAndFlush(entity);
		return entity;
	}

	/**
	 * Apply the credential expiry entered by an administrator. A detected expiry wins: the tool plug-in reads the
	 * expiry of the credentials really in use.
	 */
	private void applyManualCredentialExpiry(final NodeEditionVo vo, final Node entity) {
		// A client without this feature does not send it: the stored expiry is kept
		if (vo.isCredentialExpirySet() && entity.getCredentialExpirySource() != CredentialExpirySource.DETECTED) {
			entity.setCredentialExpiry(vo.getCredentialExpiry());
			entity.setCredentialExpirySource(vo.getCredentialExpiry() == null ? null : CredentialExpirySource.MANUAL);
		}
	}

	/**
	 * Check the mode change of an existing node keeps serving the subscriptions created on it: the mode is unchanged,
	 * or a LINK or CREATE node is widened to ALL.
	 *
	 * @param current The current mode of the node.
	 * @param mode    The requested mode.
	 */
	private void checkModeTransition(final SubscriptionMode current, final SubscriptionMode mode) {
		if (current != mode && !(mode == SubscriptionMode.ALL
				&& (current == SubscriptionMode.LINK || current == SubscriptionMode.CREATE))) {
			throw new ValidationJsonException("mode", "invalid-mode-transition", "from", current, "to", mode);
		}
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
	 * Check the connection of a node instance configuration before it is saved: the given values replace the stored
	 * ones, an untouched value keeps the stored one. Nothing is persisted and no status event is registered. The
	 * principal must be allowed to save this configuration: write access to the node, or to its parent for a new node.
	 *
	 * @param vo The node being created or edited, the payload of the save.
	 * @return The check result, holding no parameter value.
	 */
	@POST
	@Path("status/check")
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public NodeCheckVo checkConnection(final NodeEditionVo vo) {
		// The checks of a save: write access, parent of the identifier, accepted parameters
		if (repository.existsById(vo.getId())) {
			checkWritableNode(vo.getId());
		} else if (vo.isRefining()) {
			checkWritableNode(vo.getRefined());
		} else if (!securityHelper.isAdmin()) {
			throw new BusinessException("read-only-node", "node", vo.getId());
		}
		final var node = new Node();
		node.setId(vo.getId());
		node.setRefined(checkRefined(vo));
		checkInputParameters(vo).forEach(p -> pvResource.checkOwnership(p, node));

		final var result = new NodeCheckVo();
		final var plugin = locator.getResourceExpected(vo.getRefined(), ToolPlugin.class);
		if (!checksGivenValues(plugin)) {
			// Its status reads the stored configuration of the node, or there is none: "not checked", never "succeeded"
			result.setMessage("node-check-unsupported");
			return result;
		}
		try {
			final var parameters = pvResource.toCheckParameters(vo.getId(), vo.getParameters());
			result.setUp(plugin.checkConnection(vo.getId(), parameters));
		} catch (final ValidationJsonException e) {
			result.setErrors(e.getErrors());
		} catch (final BusinessException e) {
			result.setMessage(e.getMessage());
		} catch (final Exception e) { // NOSONAR: any failure of the tool is a failed check
			// Only the type: a message may hold a secret, such as an URL with credentials
			log.info("Connection check of node {} failed with {}", vo.getId(), e.getClass().getName());
			result.setMessage("node-check-failed");
		}
		return result;
	}

	/**
	 * Whether a tool plug-in checks a connection on the given values: it implements
	 * {@link ToolPlugin#checkConnection(String, Map)}, or the anonymous {@link ToolPlugin#checkStatus(Map)} the default
	 * one calls. Otherwise, its check would be the default <code>true</code> without any connection.
	 *
	 * @param plugin The tool plug-in, possibly proxied.
	 * @return <code>true</code> when the plug-in checks the given values.
	 */
	static boolean checksGivenValues(final ToolPlugin plugin) {
		final var type = AopUtils.getTargetClass(plugin);
		try {
			return type.getMethod("checkConnection", String.class, Map.class).getDeclaringClass() != ToolPlugin.class
					|| type.getMethod("checkStatus", Map.class).getDeclaringClass() != ToolPlugin.class;
		} catch (final NoSuchMethodException e) {
			// Not a tool plug-in of this API
			return false;
		}
	}

	/**
	 * Check status of each node.
	 *
	 * @param nodes The nodes to check.
	 */
	private void checkNodesStatus(final List<Node> nodes) {
		nodes.forEach(node -> {
			checkNodeStatus(node);
			releaseEntities();
		});
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
		if (status.isUp()) {
			checkCredentialExpiry(node, parameters);
			checkVersion(node, parameters);
		}
		if (eventResource.registerEvent(node, EventType.STATUS, status.name())) {
			checkSubscriptionStatus(node, status);
		}
		return status;
	}

	/**
	 * Store the credential expiry of an UP node, so with working credentials, as its tool plug-in reads it.
	 * {@link Instant#MAX} means the credentials never expire. When the tool does not tell, the stored expiry is kept;
	 * a detected one becomes manual, so an administrator can fix it once the tool no longer exposes it.
	 */
	private void checkCredentialExpiry(final Node node, final Map<String, String> parameters) {
		final var expiry = self.checkCredentialExpiry(node.getId(), parameters);
		if (expiry != null) {
			repository.updateCredentialExpiry(node.getId(), Instant.MAX.equals(expiry) ? null : expiry,
					CredentialExpirySource.DETECTED);
		} else if (node.getCredentialExpirySource() == CredentialExpirySource.DETECTED) {
			final var stored = node.getCredentialExpiry();
			repository.updateCredentialExpiry(node.getId(), stored, stored == null ? null : CredentialExpirySource.MANUAL);
		}
	}

	/**
	 * Read the credential expiry of a node from its tool plug-in. This method need to be public for the CGLIB proxying.
	 *
	 * @param node       The node identifier.
	 * @param parameters Node parameters used to call the tool.
	 * @return The expiry, {@link Instant#MAX} when the credentials never expire, or <code>null</code> when unknown,
	 *         including on failure.
	 */
	public Instant checkCredentialExpiry(final String node, final Map<String, String> parameters) {
		try {
			return locator.getResourceExpected(node, ToolPlugin.class).getCredentialExpiry(node, parameters);
		} catch (final Exception e) { // NOSONAR
			// Do not pollute logs with this failures, the expiry stays as is
			log.warn("Credential expiry of node {} failed with {}: {}", node, e.getClass(), e.getMessage());
			return null;
		}
	}

	/**
	 * Store the version of the tool an UP node calls, as its tool plug-in reads it. When the tool does not tell, the
	 * last detected version is kept. A version longer than the column, such as a page returned by a proxy, is
	 * truncated.
	 */
	private void checkVersion(final Node node, final Map<String, String> parameters) {
		final var version = StringUtils.truncate(StringUtils.trimToNull(self.checkVersion(node.getId(), parameters)),
				Node.VERSION_LENGTH);
		if (version != null && !version.equals(node.getVersion())) {
			repository.updateVersion(node.getId(), version);
		}
	}

	/**
	 * Read the version of the tool a node calls from its tool plug-in. This method need to be public for the CGLIB
	 * proxying.
	 *
	 * @param node       The node identifier.
	 * @param parameters Node parameters used to call the tool.
	 * @return The version, or <code>null</code> when unknown, including on failure.
	 */
	public String checkVersion(final String node, final Map<String, String> parameters) {
		try {
			return locator.getResourceExpected(node, ToolPlugin.class).getVersion(parameters);
		} catch (final Exception e) { // NOSONAR
			// Do not pollute logs with this failures, the version stays as is
			log.warn("Version of node {} failed with {}: {}", node, e.getClass(), e.getMessage());
			return null;
		}
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
	 * Periodically, according to the <code>health.subscription</code> cron, check status of each subscription. In a cluster, only one
	 * member runs it: the run is skipped while another member holds the lock, see {@link #HEALTH_LOCK_CACHE}.
	 */
	@Scheduled(cron = "${health.subscription}")
	public void checkSubscriptionsStatusScheduler() {
		runLocked("health.subscription", () -> checkSubscriptionsStatus(repository.findAllInstance()));
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
			releaseEntities();
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
			result.computeIfAbsent((Subscription) entityTab[0], _ -> new HashMap<>()).put(value.getParameter().getId(),
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
			final var lastValues = findLastValues(subscriptions.keySet());
			subscriptions.forEach((s, ignored) -> eventResource.registerEvent(s, EventType.STATUS,
					NodeStatus.DOWN.name(), lastValues.get(s.getId())));
		}
	}

	/**
	 * Check status for a subscription.
	 *
	 * <p>
	 * Runs in its own transaction: a failing plug-in call (for instance a query rejected by the database) marks its
	 * transaction as rollback-only even though the exception is caught here. Sharing the transaction of the caller
	 * would then abort the whole scheduled check of every node with an {@link org.springframework.transaction.UnexpectedRollbackException},
	 * and drop the status events already registered.
	 *
	 * @param subscription Subscription entity.
	 * @param parameters   Parameters of a subscription.
	 * @return status of given subscription.
	 */
	@Transactional(Transactional.TxType.REQUIRES_NEW)
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
		final var lastValues = findLastValues(subscriptions.keySet());
		for (final var subscription : subscriptions.entrySet()) {
			// For each subscription, check status
			log.info("Check all subscriptions of node {} : {}/{} ...", node.getId(), counter + 1, subscriptions.size());
			final var parameters = new HashMap<>(nodeParameters);
			parameters.putAll(subscription.getValue());
			final var subscriptionStatus = self.checkSubscriptionStatus(subscription.getKey(), parameters).getStatus();
			eventResource.registerEvent(subscription.getKey(), EventType.STATUS, subscriptionStatus.name(),
					lastValues.get(subscription.getKey().getId()));
			counter++;
		}
	}

	/**
	 * Return the last status of the given subscriptions, in a single lookup instead of one per subscription.
	 */
	private Map<Integer, String> findLastValues(final Collection<Subscription> subscriptions) {
		return eventResource.findLastValues(subscriptions.stream().map(Subscription::getId).toList(), EventType.STATUS);
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
	 * @return For each visible node, the total amount of subscriptions and the amount of subscriptions per last known
	 *         status.
	 */
	@GET
	@Path("status/subscription")
	public List<NodeStatisticsVo> getNodeStatistics() {
		final var results = new HashMap<String, NodeStatisticsVo>();

		// Aggregate first, then keep the visible nodes: the visibility is checked once per node, not per subscription
		final var visible = new HashSet<>(repository.findAllVisibleIds(securityHelper.getLogin()));

		// Map node and amount of subscriptions
		for (final var totalSubscription : repository.countNodeSubscriptions()) {
			if (visible.contains(totalSubscription[0])) {
				final var result = new NodeStatisticsVo((String) totalSubscription[0]);
				result.getValues().put("total", (Long) totalSubscription[1]);
				results.put(result.getNode(), result);
			}
		}

		// Map status of each subscription
		for (final var subscriptionsSpecificEvent : eventRepository.countSubscriptionsEvents()) {
			if (visible.contains(subscriptionsSpecificEvent[0])) {
				final var result = results.computeIfAbsent((String) subscriptionsSpecificEvent[0], NodeStatisticsVo::new);
				result.getValues().put((String) subscriptionsSpecificEvent[1], (Long) subscriptionsSpecificEvent[2]);
			}
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
				.map(n -> {
					final var vo = NodeHelper.toVoLight(n, locator);
					vo.setCredentialExpiryLevel(CredentialExpiryPolicy.getLevel(n.getCredentialExpiry(), Instant.now(),
							credentialExpiryPolicy.getWarningDays()));
					return vo;
				}).orElseThrow(() -> new ValidationJsonException("id",
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
	 * @param uriInfo  Pagination data.
	 * @param criteria The optional criteria to match.
	 * @param refined  The optional exact parent identifier. Special attention for 'service' value corresponding to the
	 *                 root, so matching the nodes without parent.
	 * @param mode     Expected subscription mode. When <code>null</code>, the node's mode is not checked.
	 * @param depth    The maximal depth. <code>0</code> means no refined, so services only. <code>1</code> means at
	 *                 most one refined level, so services and tools. For the other cases, there is no limit, and
	 *                 corresponds to the default behavior.
	 * @param status   When true, the node status are retrieved too.
	 * @return All visible nodes with the hierarchy but without UI data.
	 */
	@GET
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public TableItem<NodeVo> findAll(@Context final UriInfo uriInfo,
			@QueryParam(DataTableAttributes.SEARCH) final String criteria, @QueryParam("refined") final String refined,
			@QueryParam("mode") final SubscriptionMode mode, @QueryParam("depth") @DefaultValue("-1") final int depth,
			@QueryParam("status") @DefaultValue("false") final boolean status) {
		final var login = securityHelper.getLogin();
		final var findAll = repository.findAllVisible(login, StringUtils.trimToEmpty(criteria),
				refined, mode, depth, paginationJson.getPageRequest(uriInfo, ORM_MAPPING));

		// Optionally surface the last known status from the most recent STATUS event,
		// fetched in a SINGLE bulk query for the whole page (avoids an N+1).
		final var statuses = status ? toLastStatuses(login, findAll.getContent()) : Map.<String, NodeStatus>of();

		// The urgency of the credential expiry, with one threshold for the whole page
		final var now = Instant.now();
		final var warningDays = credentialExpiryPolicy.getWarningDays();

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, n -> {
			final var vo = NodeHelper.toVo(n, locator);
			vo.setCredentialExpiryLevel(CredentialExpiryPolicy.getLevel(n.getCredentialExpiry(), now, warningDays));
			if (status) {
				vo.setStatus(statuses.get(n.getId()));
			}
			return vo;
		});
	}

	/**
	 * {@link #findAll(UriInfo, String, String, SubscriptionMode, int, boolean)} without the optional status flag —
	 * convenience overload (equivalent to {@code status = false}) kept for the internal and test callers.
	 *
	 * @param uriInfo  Filter data.
	 * @param criteria the optional criteria to match.
	 * @param refined  the optional parent node identifier.
	 * @param mode     the optional subscription mode.
	 * @param depth    the maximum depth of refining. <code>-1</code> for an unbounded depth.
	 * @return All visible nodes matching the criteria.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public TableItem<NodeVo> findAll(final UriInfo uriInfo, final String criteria, final String refined,
			final SubscriptionMode mode, final int depth) {
		return findAll(uriInfo, criteria, refined, mode, depth, false);
	}

	/**
	 * Return the last known {@link NodeStatus} of each given node from its most recent status event, resolved with a
	 * single bulk query (avoids an N+1 over the page).
	 *
	 * @param user  The principal requesting the data (for visibility).
	 * @param nodes The nodes of the current page.
	 * @return A map of node identifier to its last known status; nodes without a parsable status event are absent.
	 */
	private Map<String, NodeStatus> toLastStatuses(final String user, final List<Node> nodes) {
		if (nodes.isEmpty()) {
			return Map.of();
		}
		final var ids = nodes.stream().map(Node::getId).toList();
		final var statuses = new HashMap<String, NodeStatus>();
		for (final var event : eventRepository.findLastEvents(user, ids)) {
			try {
				statuses.put(event.getNode().getId(), NodeStatus.valueOf(event.getValue()));
			} catch (final IllegalArgumentException _) {
				// Unknown / non-status value — ignore.
			}
		}
		return statuses;
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
	 * Check the parameters that are being attached to this node do not override a parameter already valued by the
	 * parent nodes. Mandatory parameters and ownerships are not checked there.
	 *
	 * @param vo The parameterized object. A <code>null</code> parameter list is replaced by an empty one.
	 * @return The accepted {@link Parameter} entities: the ones of the node's hierarchy not yet valued, for the given
	 *         mode.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
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
	protected <
			T extends AbstractLongTask<Node, String>,
			R extends LongTaskRepository<T, Node, String>,
			A extends RestRepository<Node, String>,
			S extends AbstractLockedResource<Node, String>
			> Class<? extends LongTaskRunner<T, R, Node, String, A, S>> getLongTaskRunnerClass() {
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

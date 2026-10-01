/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link Event} repository
 */
@SuppressWarnings("ALL")
public interface EventRepository extends RestRepository<Event, String> {

	/**
	 * find the last event for a node and a type
	 *
	 * @param node node
	 * @param type event type
	 * @return last event
	 */
	@SuppressWarnings("unused")
	Event findFirstByNodeAndTypeOrderByIdDesc(Node node, EventType type);

	/**
	 * find the last event for a subscription and a type
	 *
	 * @param subscription subscription
	 * @param type         event type
	 * @return last event
	 */
	@SuppressWarnings("unused")
	Event findFirstBySubscriptionAndTypeOrderByIdDesc(Subscription subscription, EventType type);

	/**
	 * Return last events of all visible nodes for a given user.
	 *
	 * @param user The user requesting the nodes.
	 * @return last events of all nodes.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT event FROM Event event INNER JOIN FETCH event.node n INNER JOIN FETCH n.refined tool INNER JOIN tool.refined root"
			+ " WHERE event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.node = n) AND "
			+ NodeRepository.VISIBLE_NODES)
	List<Event> findLastEvents(String user);

	/**
	 * Return the last event if available of a visible node for a given user.
	 *
	 * @param user The principal user requesting the nodes.
	 * @param node The related node.
	 * @return last events of a specific node.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT e FROM Event e INNER JOIN e.node n WHERE e.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.node = n) AND n.id = :node AND"
			+ NodeRepository.VISIBLE_NODES)
	Event findLastEvent(String user, String node);

	/**
	 * Return the last event, if any, of each given visible node for a user, in a single query (bulk variant of
	 * {@link #findLastEvent(String, String)} to avoid an N+1 over a node list).
	 *
	 * @param user  The principal user requesting the nodes.
	 * @param nodes The related node identifiers.
	 * @return the last event of each matching node; nodes without an event are simply absent.
	 */
	@SuppressWarnings("unused")
	@Query("SELECT e FROM Event e INNER JOIN e.node n WHERE e.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.node = n) AND n.id IN :nodes AND"
			+ NodeRepository.VISIBLE_NODES)
	List<Event> findLastEvents(String user, Collection<String> nodes);

	/**
	 * find last events for a project
	 *
	 * @param project Project identifier.
	 * @return all events
	 */
	@SuppressWarnings("unused")
	@Query("SELECT event FROM Subscription sub, Event event "
			+ " WHERE sub.project.id = :project AND event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.subscription = sub)")
	List<Event> findLastEvents(int project);

	/**
	 * count subscriptions events grouped by node and value
	 *
	 * @param user The user requesting the nodes.
	 * @return subscriptions events count
	 * @deprecated Visibility checked per subscription row: use {@link #countSubscriptionsEvents()} filtered with
	 *             {@link NodeRepository#findAllVisibleIds(String)}, as {@code NodeResource#getNodeStatistics()} does.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	@Query("SELECT n.id, event.value, count(event) FROM Event event INNER JOIN event.subscription sub LEFT JOIN sub.node n"
			+ " WHERE event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.subscription = sub) AND "
			+ NodeRepository.VISIBLE_NODES + " GROUP BY event.value, n.id")
	List<Object[]> countSubscriptionsEvents(String user);

	/**
	 * Count subscriptions events grouped by node and value, without visibility check: to be filtered with
	 * {@link NodeRepository#findAllVisibleIds(String)}.
	 *
	 * @return subscriptions events count: node identifier, event value, count.
	 */
	@Query("SELECT sub.node.id, event.value, count(event) FROM Subscription sub, Event event"
			+ " WHERE event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.subscription = sub)"
			+ " GROUP BY event.value, sub.node.id")
	List<Object[]> countSubscriptionsEvents();

	/**
	 * Return the value of the last event of a type for each given subscription, in a single query.
	 *
	 * @param subscriptions The subscription identifiers.
	 * @param type          The event type.
	 * @return The subscription identifier and the last event value. Subscriptions without event are absent.
	 */
	@Query("SELECT sub.id, e.value FROM Subscription sub, Event e WHERE sub.id IN :subscriptions"
			+ " AND e.id = (SELECT MAX(x.id) FROM Event x WHERE x.subscription = sub AND x.type = :type)")
	List<Object[]> findLastValues(Collection<Integer> subscriptions, EventType type);

	/**
	 * Return the identifiers of the events older than the given date and followed by a newer event of the same node
	 * or subscription: the last event of each node and subscription is never returned.
	 *
	 * @param before Exclusive upper bound of the event date.
	 * @return The identifiers of the replaced expired events.
	 */
	@Query("SELECT e.id FROM Event e WHERE e.date < :before AND EXISTS(SELECT 1 FROM Event x WHERE x.id > e.id AND"
			+ " (x.node.id = e.node.id OR x.subscription.id = e.subscription.id))")
	List<Integer> findAllReplacedBefore(Instant before);

	/**
	 * Delete the events by their identifiers.
	 *
	 * @param ids The event identifiers.
	 * @return The amount of deleted events.
	 */
	@Modifying
	@Query("DELETE FROM Event WHERE id IN :ids")
	int deleteAllByIds(Collection<Integer> ids);

	/**
	 * Delete all events related to the given node.
	 *
	 * @param node The node identifier.
	 */
	@SuppressWarnings("unused")
	@Modifying
	@Query("DELETE Event WHERE node.id = :node OR node.id LIKE CONCAT(:node, ':%')"
			+ " OR subscription.id IN (SELECT id FROM Subscription WHERE node.id = :node OR node.id LIKE CONCAT(:node, ':%'))")
	void deleteByNode(String node);
}

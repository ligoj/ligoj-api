/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

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
			+ " WHERE event.id = (SELECT MAX(cast(lastEvent.id as Integer)) FROM Event lastEvent WHERE lastEvent.node = n) AND "
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
	@Query("FROM Event e INNER JOIN e.node n WHERE e.id = (SELECT MAX(cast(lastEvent.id as Integer)) FROM Event lastEvent WHERE lastEvent.node = n) AND n.id = :node AND"
			+ NodeRepository.VISIBLE_NODES)
	Event findLastEvent(String user, String node);

	/**
	 * find last events for a project
	 *
	 * @param project Project identifier.
	 * @return all events
	 */
	@SuppressWarnings("unused")
	@Query("SELECT event FROM Event event INNER JOIN FETCH event.subscription sub "
			+ " WHERE sub.project.id = :project AND event.id = (SELECT MAX(cast(lastEvent.id as Integer)) FROM Event lastEvent WHERE lastEvent.subscription = sub)")
	List<Event> findLastEvents(int project);

	/**
	 * count subscriptions events grouped by node and value
	 *
	 * @param user The user requesting the nodes.
	 * @return subscriptions events count
	 */
	@SuppressWarnings("unused")
	@Query("SELECT n.id, event.value, count(event) FROM Event event INNER JOIN event.subscription sub LEFT JOIN sub.node n"
			+ " WHERE event.id = (SELECT MAX(cast(lastEvent.id as Integer)) FROM Event lastEvent WHERE lastEvent.subscription = sub) AND "
			+ NodeRepository.VISIBLE_NODES + " GROUP BY event.value, n.id")
	List<Object[]> countSubscriptionsEvents(String user);

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

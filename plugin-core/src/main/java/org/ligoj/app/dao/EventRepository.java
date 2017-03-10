package org.ligoj.app.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;

/**
 * {@link Event} repository
 */
public interface EventRepository extends RestRepository<Event, String> {

	/**
	 * find the last event for a node and a type
	 * 
	 * @param node
	 *            node
	 * @param type
	 *            event type
	 * @return last event
	 */
	Event findFirstByNodeAndTypeOrderByIdDesc(Node node, EventType type);

	/**
	 * find the last event for a subscription and a type
	 * 
	 * @param subscription
	 *            subscription
	 * @param type
	 *            event type
	 * @return last event
	 */
	Event findFirstBySubscriptionAndTypeOrderByIdDesc(Subscription subscription, EventType type);

	/**
	 * Return last events of all visible nodes for a given user.
	 * 
	 * @param user
	 *            The user requesting the nodes.
	 * @return last events of all nodes.
	 */
	@Query("SELECT event FROM Event event INNER JOIN FETCH event.node n INNER JOIN FETCH n.refined tool INNER JOIN tool.refined root"
			+ " WHERE event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.node = n) AND " + NodeRepository.VISIBLE_NODES)
	List<Event> findLastEvents(String user);

	/**
	 * find last events for a project
	 * 
	 * @param project
	 *            Project identifier.
	 * @return all events
	 */
	@Query("SELECT event FROM Event event INNER JOIN FETCH event.subscription sub "
			+ " WHERE sub.project.id = :project AND event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.subscription = sub)")
	List<Event> findLastEvents(int project);

	/**
	 * count subscriptions events grouped by node and value
	 * 
	 * @param user
	 *            The user requesting the nodes.
	 * @return subscriptions events count
	 */
	@Query("SELECT n.id, event.value, count(event) FROM Event event INNER JOIN event.subscription sub LEFT JOIN sub.node n"
			+ " WHERE event.id = (SELECT MAX(lastEvent.id) FROM Event lastEvent WHERE lastEvent.subscription = sub) AND "
			+ NodeRepository.VISIBLE_NODES + " GROUP BY event.value, n.id")
	List<Object[]> countSubscriptionsEvents(String user);
}

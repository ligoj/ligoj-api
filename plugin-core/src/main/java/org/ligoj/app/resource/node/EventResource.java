/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * {@link Event} resource.
 */
@Service
@Transactional
@Slf4j
public class EventResource {

	/**
	 * Maximal amount of identifiers in a single statement.
	 */
	private static final int MAX_IDS = 1000;

	@Autowired
	private EventRepository repository;

	/**
	 * Retention of the replaced events, in days. Older events are deleted by {@link #purge()}, except the last one of
	 * each node and subscription.
	 */
	@Value("${event.retention:365}")
	private int retention = 365;

	/**
	 * Return the value of the last event of a type for each given subscription. The lookup is chunked to keep the
	 * statements small.
	 *
	 * @param subscriptions The subscription identifiers.
	 * @param eventType     The event type.
	 * @return The last event value by subscription identifier. Subscriptions without event are absent.
	 */
	public Map<Integer, String> findLastValues(final Collection<Integer> subscriptions, final EventType eventType) {
		final var ids = new ArrayList<>(subscriptions);
		final var result = new HashMap<Integer, String>();
		for (var from = 0; from < ids.size(); from += MAX_IDS) {
			repository.findLastValues(ids.subList(from, Math.min(from + MAX_IDS, ids.size())), eventType)
					.forEach(r -> result.put((Integer) r[0], (String) r[1]));
		}
		return result;
	}

	/**
	 * Delete the events older than the retention period (<code>event.retention</code> days, 365 by default), except the
	 * last event of each node and subscription, holding its current status. Scheduled daily (<code>event.purge</code>
	 * cron, 4 AM by default). The deletion is chunked to keep the statements small.
	 *
	 * @return The amount of deleted events.
	 */
	@Scheduled(cron = "${event.purge:0 0 4 * * ?}")
	public int purge() {
		final var ids = repository.findAllReplacedBefore(Instant.now().minus(retention, ChronoUnit.DAYS));
		var count = 0;
		for (var from = 0; from < ids.size(); from += MAX_IDS) {
			count += repository.deleteAllByIds(ids.subList(from, Math.min(from + MAX_IDS, ids.size())));
		}
		log.info("Purged {} events older than {} days", count, retention);
		return count;
	}

	/**
	 * Register an event on a node. The event will be registered only if the value is new.
	 *
	 * @param node      node
	 * @param eventType event type
	 * @param value     new value
	 * @return <code>true</code> if the event has been registered in database.
	 */
	public boolean registerEvent(final Node node, final EventType eventType, final String value) {
		final var lastEvent = repository.findFirstByNodeAndTypeOrderByIdDesc(node, eventType);

		// Register event if it is a discovered node, or a status change
		if (lastEvent == null || !value.equals(lastEvent.getValue())) {
			final var newEvent = new Event();
			newEvent.setNode(node);
			saveEvent(newEvent, eventType, value);
			return true;
		}

		// No change, no persisted event
		return false;
	}

	/**
	 * register an event on a subscription. The event will be registered only if the value is new.
	 *
	 * @param subscription The related subscription.
	 * @param eventType    The new event type.
	 * @param value        The new event value.
	 * @return <code>true</code> if an event has been saved in database.
	 */
	public boolean registerEvent(final Subscription subscription, final EventType eventType, final String value) {
		final var lastEvent = repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, eventType);
		return registerEvent(subscription, eventType, value, lastEvent == null ? null : lastEvent.getValue());
	}

	/**
	 * Register an event on a subscription when the value differs from the already known last value: to use with
	 * {@link #findLastValues(Collection, EventType)} when many subscriptions are checked.
	 *
	 * @param subscription The related subscription.
	 * @param eventType    The new event type.
	 * @param value        The new event value.
	 * @param lastValue    The value of the last event of this type, <code>null</code> when there is none.
	 * @return <code>true</code> if an event has been saved in database.
	 */
	public boolean registerEvent(final Subscription subscription, final EventType eventType, final String value,
			final String lastValue) {
		if (!value.equals(lastValue)) {
			final var newEvent = new Event();
			newEvent.setSubscription(subscription);
			saveEvent(newEvent, eventType, value);
			return true;
		}
		return false;
	}

	/**
	 * save an event
	 *
	 * @param event     event
	 * @param eventType event Type
	 * @param value     value
	 */
	private void saveEvent(final Event event, final EventType eventType, final String value) {
		event.setValue(value);
		event.setType(eventType);
		event.setDate(Instant.now());
		repository.save(event);
	}

	/**
	 * {@link Event} JPA to VO object transformer without refined information.
	 *
	 * @param entity Source entity.
	 * @return The corresponding VO object with node/subscription reference.
	 */
	public static EventVo toVo(final Event entity) {
		final var vo = new EventVo();
		vo.setValue(entity.getValue());
		vo.setType(entity.getType());
		if (entity.getNode() == null) {
			vo.setSubscription(entity.getSubscription().getId());
			vo.setNode(NodeHelper.toVoLight(entity.getSubscription().getNode()));
		} else {
			vo.setNode(NodeHelper.toVoLight(entity.getNode()));
		}
		return vo;
	}

	/**
	 * Return the last known event related to a visible node of given user.
	 *
	 * @param user The principal user requesting these events.
	 * @param node The related node.
	 * @return Event related to a visible {@link Node}. May be <code>null</code>.
	 */
	public EventVo findByNode(final String user, final String node) {
		return Optional.ofNullable(repository.findLastEvent(user, node)).map(EventResource::toVo).orElse(null);
	}

	/**
	 * Return all events related to a visible node of given user.
	 *
	 * @param user The principal user requesting these events.
	 * @return Events related to a visible {@link Node}.
	 */
	public List<EventVo> findAll(final String user) {
		final var events = repository.findLastEvents(user);
		final var services = new HashMap<String, EventVo>();
		final var tools = new HashMap<String, EventVo>();
		for (final var event : events) {
			final var parent = event.getNode().getRefined();
			fillParentEvents(tools, parent, EventResource.toVo(event), event.getValue());
			fillParentEvents(services, parent.getRefined(), tools.get(parent.getId()), event.getValue());
		}
		return new ArrayList<>(services.values());
	}

	private void fillParentEvents(final Map<String, EventVo> parents, final Node parent, final EventVo eventVo,
			final String eventValue) {
		final var service = parents.computeIfAbsent(parent.getId(), key -> {
			final var result = new EventVo();
			result.setNode(NodeHelper.toVoLight(parent));
			result.setValue(eventValue);
			result.setType(eventVo.getType());
			return result;
		});
		service.getSpecifics().add(eventVo);
		if ("DOWN".equals(eventValue)) {
			service.setValue(eventValue);
		}
	}
}

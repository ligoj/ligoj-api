/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@link Event} resource.
 */
@Service
@Transactional
public class EventResource {

	@Autowired
	private EventRepository repository;

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
		if (lastEvent == null || !value.equals(lastEvent.getValue())) {
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
		event.setDate(new Date());
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
			vo.setNode(NodeResource.toVoLight(entity.getSubscription().getNode()));
		} else {
			vo.setNode(NodeResource.toVoLight(entity.getNode()));
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
			result.setNode(NodeResource.toVoLight(parent));
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

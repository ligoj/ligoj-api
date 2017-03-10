package org.ligoj.app.resource.node;

import java.util.Date;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;

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
	 * @param node
	 *            node
	 * @param eventType
	 *            event type
	 * @param value
	 *            new value
	 * @return <code>true</code> if the event has been registered in database.
	 */
	public boolean registerEvent(final Node node, final EventType eventType, final String value) {
		final Event lastEvent = repository.findFirstByNodeAndTypeOrderByIdDesc(node, eventType);

		// Register event if it is a discovered node, or a status change
		if (lastEvent == null || !value.equals(lastEvent.getValue())) {
			final Event newEvent = new Event();
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
	 * @param subscription
	 *            subscription
	 * @param eventType
	 *            event type
	 * @param value
	 *            new value
	 * @return true if an event has been saved in database
	 */
	public boolean registerEvent(final Subscription subscription, final EventType eventType, final String value) {
		final Event lastEvent = repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, eventType);
		if (lastEvent == null || !value.equals(lastEvent.getValue())) {
			final Event newEvent = new Event();
			newEvent.setSubscription(subscription);
			saveEvent(newEvent, eventType, value);
			return true;
		}
		return false;
	}

	/**
	 * save an event
	 * 
	 * @param event
	 *            event
	 * @param eventType
	 *            event Type
	 * @param value
	 *            value
	 */
	private void saveEvent(final Event event, final EventType eventType, final String value) {
		event.setValue(value);
		event.setType(eventType);
		event.setDate(new Date());
		repository.save(event);
	}

	/**
	 * {@link Event} JPA to VO object transformer without refined informations.
	 * 
	 * @param entity
	 *            Source entity.
	 * @return The corresponding VO object with node/subscription reference.
	 */
	public static EventVo toVo(final Event entity) {
		final EventVo vo = new EventVo();
		vo.setValue(entity.getValue());
		vo.setType(entity.getType());
		if (entity.getNode() == null) {
			vo.setSubscription(entity.getSubscription().getId());
			vo.setLabel(entity.getSubscription().getNode().getName());
		} else {
			vo.setNode(entity.getNode().getId());
			vo.setLabel(entity.getNode().getName());
		}
		return vo;
	}

}

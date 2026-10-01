/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@link NodeResource} test cases.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class EventResourceTest extends AbstractAppTest {

	@Autowired
	private EventResource resource;

	@Autowired
	private EventRepository repository;

	@Autowired
	private ProjectRepository projectRepository;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class, Event.class }, StandardCharsets.UTF_8);
	}

	@Test
	void registerNodeEvent() {
		final var node = new Node();
		node.setId("junit1");
		node.setName("junit1");
		em.persist(node);

		var count = repository.count();
		Assertions.assertTrue(resource.registerEvent(node, EventType.STATUS, NodeStatus.UP.name()));
		Assertions.assertEquals(++count, repository.count());
		Assertions.assertTrue(resource.registerEvent(node, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(++count, repository.count());
		final var lastEvent = repository.findFirstByNodeAndTypeOrderByIdDesc(node, EventType.STATUS);
		Assertions.assertTrue(lastEvent.getDate().plusSeconds(5).isAfter(Instant.now()));
		Assertions.assertFalse(resource.registerEvent(node, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(count, repository.count());
		Assertions.assertEquals(lastEvent, repository.findFirstByNodeAndTypeOrderByIdDesc(node, EventType.STATUS));
	}

	@Test
	void registerSubscriptionEvent() {
		final var subscription = new Subscription();
		subscription.setProject(projectRepository.findByName("MDA"));
		subscription.setNode(em.find(Node.class, "service:build:jenkins:bpr"));
		em.persist(subscription);
		var count = repository.count();
		Assertions.assertTrue(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.UP.name()));
		Assertions.assertEquals(++count, repository.count());
		Assertions.assertTrue(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(++count, repository.count());
		final var lastEvent = repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, EventType.STATUS);
		Assertions.assertTrue(lastEvent.getDate().plusSeconds(5).isAfter(Instant.now()));
		Assertions.assertFalse(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(count, repository.count());
		Assertions.assertEquals(lastEvent,
				repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, EventType.STATUS));
	}

	private Event newEvent(final Node node, final Subscription subscription, final String value, final Instant date) {
		final var event = new Event();
		event.setNode(node);
		event.setSubscription(subscription);
		event.setType(EventType.STATUS);
		event.setValue(value);
		event.setDate(date);
		em.persist(event);
		return event;
	}

	@Test
	void purge() {
		final var old = Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS);
		final var node = new Node();
		node.setId("service:bt:jira:purge");
		node.setName("purge");
		node.setRefined(em.find(Node.class, "service:bt:jira"));
		em.persist(node);
		final var subscription = new Subscription();
		subscription.setProject(projectRepository.findByName("MDA"));
		subscription.setNode(node);
		em.persist(subscription);
		final var initial = repository.count();

		// Expired and replaced: deleted
		final var e1 = newEvent(node, null, "DOWN", old);
		final var e2 = newEvent(null, subscription, "DOWN", old);
		// Recent: kept
		final var e3 = newEvent(node, null, "UP", Instant.now());
		// Expired but the last one of its subscription: kept
		final var e4 = newEvent(null, subscription, "UP", old);
		em.flush();

		// Default retention: 365 days. The fixture events (2013) are the last ones of their target: kept
		Assertions.assertEquals(2, resource.purge());
		em.flush();
		em.clear();
		Assertions.assertNull(em.find(Event.class, e1.getId()));
		Assertions.assertNull(em.find(Event.class, e2.getId()));
		Assertions.assertNotNull(em.find(Event.class, e3.getId()));
		Assertions.assertNotNull(em.find(Event.class, e4.getId()));
		Assertions.assertEquals(initial + 2, repository.count());
	}

	@Test
	void findLastValues() {
		final var subscription = new Subscription();
		subscription.setProject(projectRepository.findByName("MDA"));
		subscription.setNode(em.find(Node.class, "service:bt:jira:6"));
		em.persist(subscription);
		final var other = new Subscription();
		other.setProject(projectRepository.findByName("MDA"));
		other.setNode(em.find(Node.class, "service:bt:jira:6"));
		em.persist(other);
		newEvent(null, subscription, "DOWN", Instant.now());
		newEvent(null, subscription, "UP", Instant.now());
		em.flush();

		final var values = resource.findLastValues(List.of(subscription.getId(), other.getId()), EventType.STATUS);
		Assertions.assertEquals(Map.of(subscription.getId(), "UP"), values);
		Assertions.assertEquals(Map.of(), resource.findLastValues(List.of(), EventType.STATUS));
	}

	@Test
	void registerSubscriptionEventKnownLastValue() {
		final var subscription = new Subscription();
		subscription.setProject(projectRepository.findByName("MDA"));
		subscription.setNode(em.find(Node.class, "service:bt:jira:6"));
		em.persist(subscription);
		final var count = repository.count();

		// Same value as the known last one: no lookup and no new event
		Assertions.assertFalse(resource.registerEvent(subscription, EventType.STATUS, "UP", "UP"));
		Assertions.assertEquals(count, repository.count());
		Assertions.assertTrue(resource.registerEvent(subscription, EventType.STATUS, "DOWN", "UP"));
		Assertions.assertTrue(resource.registerEvent(subscription, EventType.STATUS, "UP", null));
		Assertions.assertEquals(count + 2, repository.count());
	}
}

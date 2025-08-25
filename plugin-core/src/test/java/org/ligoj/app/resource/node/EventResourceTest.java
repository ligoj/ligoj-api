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
}

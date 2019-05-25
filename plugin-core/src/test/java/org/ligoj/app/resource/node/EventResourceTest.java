/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.transaction.Transactional;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
		persistEntities("csv",
				new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class, Event.class },
				StandardCharsets.UTF_8.name());
	}

	@Test
	void registerNodeEvent() {
		final Node node = new Node();
		node.setId("junit1");
		node.setName("junit1");
		em.persist(node);

		long count = repository.count();
		Assertions.assertTrue(resource.registerEvent(node, EventType.STATUS, NodeStatus.UP.name()));
		Assertions.assertEquals(++count, repository.count());
		Assertions.assertTrue(resource.registerEvent(node, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(++count, repository.count());
		final Event lastEvent = repository.findFirstByNodeAndTypeOrderByIdDesc(node, EventType.STATUS);
		Assertions.assertTrue(DateUtils.addSeconds(lastEvent.getDate(), 5).after(new Date()));
		Assertions.assertFalse(resource.registerEvent(node, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(count, repository.count());
		Assertions.assertEquals(lastEvent, repository.findFirstByNodeAndTypeOrderByIdDesc(node, EventType.STATUS));
	}

	@Test
	void registerSubscriptionEvent() {
		final Subscription subscription = new Subscription();
		subscription.setProject(projectRepository.findByName("MDA"));
		subscription.setNode(em.find(Node.class, "service:build:jenkins:bpr"));
		em.persist(subscription);
		long count = repository.count();
		Assertions.assertTrue(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.UP.name()));
		Assertions.assertEquals(++count, repository.count());
		Assertions.assertTrue(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(++count, repository.count());
		final Event lastEvent = repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, EventType.STATUS);
		Assertions.assertTrue(DateUtils.addSeconds(lastEvent.getDate(), 5).after(new Date()));
		Assertions.assertFalse(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.DOWN.name()));
		Assertions.assertEquals(count, repository.count());
		Assertions.assertEquals(lastEvent, repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, EventType.STATUS));
	}
}

package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.transaction.Transactional;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * {@link NodeResource} test cases.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EventResourceTest extends AbstractAppTest {

	@Autowired
	private EventResource resource;

	@Autowired
	private EventRepository repository;

	@Autowired
	private ProjectRepository projectRepository;

	@Before
	public void prepare() throws IOException {
		persistEntities("csv/app-test",
				new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class, Event.class },
				StandardCharsets.UTF_8.name());
	}

	@Test
	public void registerNodeEvent() {
		final Node node = new Node();
		node.setId("junit1");
		node.setName("junit1");
		em.persist(node);

		long count = repository.count();
		Assert.assertTrue(resource.registerEvent(node, EventType.STATUS, NodeStatus.UP.name()));
		Assert.assertEquals(++count, repository.count());
		Assert.assertTrue(resource.registerEvent(node, EventType.STATUS, NodeStatus.DOWN.name()));
		Assert.assertEquals(++count, repository.count());
		final Event lastEvent = repository.findFirstByNodeAndTypeOrderByIdDesc(node, EventType.STATUS);
		Assert.assertTrue(DateUtils.addSeconds(lastEvent.getDate(), 5).after(new Date()));
		Assert.assertFalse(resource.registerEvent(node, EventType.STATUS, NodeStatus.DOWN.name()));
		Assert.assertEquals(count, repository.count());
		Assert.assertEquals(lastEvent, repository.findFirstByNodeAndTypeOrderByIdDesc(node, EventType.STATUS));
	}

	@Test
	public void registerSubscriptionEvent() {
		final Subscription subscription = new Subscription();
		subscription.setProject(projectRepository.findByName("MDA"));
		subscription.setNode(em.find(Node.class, "service:build:jenkins:bpr"));
		em.persist(subscription);
		long count = repository.count();
		Assert.assertTrue(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.UP.name()));
		Assert.assertEquals(++count, repository.count());
		Assert.assertTrue(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.DOWN.name()));
		Assert.assertEquals(++count, repository.count());
		final Event lastEvent = repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, EventType.STATUS);
		Assert.assertTrue(DateUtils.addSeconds(lastEvent.getDate(), 5).after(new Date()));
		Assert.assertFalse(resource.registerEvent(subscription, EventType.STATUS, NodeStatus.DOWN.name()));
		Assert.assertEquals(count, repository.count());
		Assert.assertEquals(lastEvent, repository.findFirstBySubscriptionAndTypeOrderByIdDesc(subscription, EventType.STATUS));
	}
}

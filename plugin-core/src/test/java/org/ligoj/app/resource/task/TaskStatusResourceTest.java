/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.dao.TaskSampleNodeRepository;
import org.ligoj.app.dao.TaskSampleSubscriptionRepository;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.model.TaskSampleNode;
import org.ligoj.app.model.TaskSampleSubscription;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.node.TaskSampleNodeResource;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.subscription.TaskSampleSubscriptionResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link TaskStatusResource}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class TaskStatusResourceTest extends AbstractOrgTest {

	private static final String NODE_RUNNER = "taskSampleNodeResource";
	private static final String SUBSCRIPTION_RUNNER = "taskSampleSubscriptionResource";

	@Autowired
	private TaskStatusResource resource;

	@Autowired
	private TaskSampleNodeRepository nodeRepository;

	@Autowired
	private TaskSampleSubscriptionRepository subscriptionRepository;

	private int subscription;

	@BeforeEach
	void setUpRunners() {
		subscription = getSubscription("MDA", BugTrackerResource.SERVICE_KEY);
		// Register the sample runners as beans so getBeansOfType() discovers them.
		registerRunner(NODE_RUNNER, TaskSampleNodeResource.class);
		registerRunner(SUBSCRIPTION_RUNNER, TaskSampleSubscriptionResource.class);
	}

	@AfterEach
	void tearDownRunners() {
		destroyRunner(NODE_RUNNER);
		destroyRunner(SUBSCRIPTION_RUNNER);
	}

	private DefaultListableBeanFactory beanFactory() {
		return (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
	}

	private void registerRunner(final String name, final Class<?> type) {
		beanFactory().registerSingleton(name, beanFactory().createBean(type));
	}

	private void destroyRunner(final String name) {
		if (beanFactory().containsSingleton(name)) {
			beanFactory().destroySingleton(name);
		}
	}

	private TaskSampleNode nodeTask(final String node, final long start, final Date end, final boolean failed) {
		final var task = new TaskSampleNode();
		task.setAuthor(DEFAULT_USER);
		task.setData("d");
		task.setStart(new Date(start));
		task.setEnd(end);
		task.setFailed(failed);
		task.setLocked(em.find(Node.class, node));
		return nodeRepository.saveAndFlush(task);
	}

	private TaskSampleSubscription subscriptionTask(final Date end, final boolean failed) {
		final var task = new TaskSampleSubscription();
		task.setAuthor(DEFAULT_USER);
		task.setData("d");
		task.setStart(new Date());
		task.setEnd(end);
		task.setFailed(failed);
		task.setLocked(em.find(Subscription.class, subscription));
		return subscriptionRepository.saveAndFlush(task);
	}

	private LongTaskRunnerVo runner(final List<LongTaskRunnerVo> runners, final String key) {
		return runners.stream().filter(r -> key.equals(r.getKey())).findFirst().orElse(null);
	}

	@Test
	void findAllEnumeratesAndClassifies() {
		nodeTask("service:bt:jira", 1000, new Date(), false);
		subscriptionTask(new Date(), false);

		final var runners = resource.findAll();
		final var node = runner(runners, NODE_RUNNER);
		final var sub = runner(runners, SUBSCRIPTION_RUNNER);

		Assertions.assertNotNull(node);
		Assertions.assertEquals(TaskStatusType.NODE, node.getType());
		Assertions.assertEquals("TaskSampleNode", node.getLabel());

		Assertions.assertNotNull(sub);
		Assertions.assertEquals(TaskStatusType.SUBSCRIPTION, sub.getType());
		Assertions.assertEquals("TaskSampleSubscription", sub.getLabel());
	}

	@Test
	void findAllComputesStats() {
		nodeTask("service:bt:jira", 1000, null, false); // running
		nodeTask("service:bt:jira:4", 2000, new Date(), false); // succeeded
		nodeTask("service:bt:jira:6", 3000, new Date(), true); // failed

		final var node = runner(resource.findAll(), NODE_RUNNER);
		Assertions.assertNotNull(node);
		final var stats = node.getStats();
		Assertions.assertEquals(3, stats.total());
		Assertions.assertEquals(1, stats.running());
		Assertions.assertEquals(1, stats.succeeded());
		Assertions.assertEquals(1, stats.failed());
	}

	@Test
	void findTasksSortedByStartDesc() {
		nodeTask("service:bt:jira", 1000, new Date(), false);
		nodeTask("service:bt:jira:4", 3000, new Date(), false);
		nodeTask("service:bt:jira:6", 2000, new Date(), false);

		final var result = resource.findTasks(NODE_RUNNER, newUriInfo(), null);
		Assertions.assertEquals(3, result.getRecordsTotal());
		final var data = result.getData();
		Assertions.assertEquals(3000, data.get(0).getStart().getTime());
		Assertions.assertEquals(2000, data.get(1).getStart().getTime());
		Assertions.assertEquals(1000, data.get(2).getStart().getTime());
		// Node locked reference feeds the icon.
		Assertions.assertEquals(TaskStatusType.NODE, data.get(0).getLocked().getType());
		Assertions.assertEquals("service:bt:jira:4", data.get(0).getLocked().getNode());
	}

	@Test
	void findTasksFilteredByStatus() {
		nodeTask("service:bt:jira", 1000, null, false); // running
		nodeTask("service:bt:jira:4", 2000, new Date(), false); // succeeded
		nodeTask("service:bt:jira:6", 3000, new Date(), true); // failed

		final var running = resource.findTasks(NODE_RUNNER, newUriInfo(), "running");
		Assertions.assertEquals(1, running.getRecordsTotal());
		Assertions.assertEquals(TaskStatus.RUNNING, running.getData().getFirst().getStatus());

		final var failed = resource.findTasks(NODE_RUNNER, newUriInfo(), "failed");
		Assertions.assertEquals(1, failed.getRecordsTotal());
		Assertions.assertEquals(TaskStatus.FAILED, failed.getData().getFirst().getStatus());
	}

	@Test
	void findTasksPaginated() {
		nodeTask("service:bt:jira", 1000, new Date(), false);
		nodeTask("service:bt:jira:4", 2000, new Date(), false);
		nodeTask("service:bt:jira:6", 3000, new Date(), false);

		final var uriInfo = newUriInfo();
		uriInfo.getQueryParameters().putSingle("rows", "2");
		uriInfo.getQueryParameters().putSingle("page", "1");
		final var result = resource.findTasks(NODE_RUNNER, uriInfo, null);
		Assertions.assertEquals(3, result.getRecordsTotal());
		Assertions.assertEquals(2, result.getData().size());
	}

	@Test
	void findTasksSubscriptionLockedRef() {
		subscriptionTask(new Date(), false);
		final var result = resource.findTasks(SUBSCRIPTION_RUNNER, newUriInfo(), null);
		Assertions.assertEquals(1, result.getRecordsTotal());
		final var locked = result.getData().getFirst().getLocked();
		Assertions.assertEquals(TaskStatusType.SUBSCRIPTION, locked.getType());
		Assertions.assertEquals(subscription, locked.getSubscription().intValue());
		Assertions.assertNotNull(locked.getProject());
		Assertions.assertEquals("MDA", locked.getProject().name());
	}

	@Test
	void notVisibleForOtherUser() {
		nodeTask("service:bt:jira", 1000, new Date(), false);
		subscriptionTask(new Date(), false);
		// A user with no delegate / group membership and not admin sees nothing.
		initSpringSecurityContext("any");

		Assertions.assertEquals(0, resource.findTasks(NODE_RUNNER, newUriInfo(), null).getRecordsTotal());
		Assertions.assertEquals(0, resource.findTasks(SUBSCRIPTION_RUNNER, newUriInfo(), null).getRecordsTotal());
		Assertions.assertEquals(0, runner(resource.findAll(), NODE_RUNNER).getStats().total());
	}

	@Test
	void findTasksUnknownKey() {
		final UriInfo uriInfo = newUriInfo();
		Assertions.assertThrows(NotFoundException.class, () -> resource.findTasks("unknown", uriInfo, null));
	}
}

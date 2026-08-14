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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void findAllOtherRunner() {
		// A runner that is neither node nor subscription scoped: classified OTHER,
		// tasks listed from the plain repository, locked reference unclassified
		final var runner = org.mockito.Mockito.mock(org.ligoj.app.resource.plugin.LongTaskRunner.class);
		final var repository = org.mockito.Mockito.mock(org.ligoj.app.dao.task.LongTaskRepository.class);
		final var running = org.mockito.Mockito.mock(org.ligoj.app.model.AbstractLongTask.class);
		org.mockito.Mockito.when(running.getStart()).thenReturn(new Date(1000));
		final var succeeded = org.mockito.Mockito.mock(org.ligoj.app.model.AbstractLongTask.class);
		org.mockito.Mockito.when(succeeded.getStart()).thenReturn(new Date(2000));
		org.mockito.Mockito.when(succeeded.getEnd()).thenReturn(new Date(2500));
		final var failed = org.mockito.Mockito.mock(org.ligoj.app.model.AbstractLongTask.class);
		org.mockito.Mockito.when(failed.getStart()).thenReturn(new Date(3000));
		org.mockito.Mockito.when(failed.getEnd()).thenReturn(new Date(3500));
		org.mockito.Mockito.when(failed.isFailed()).thenReturn(true);
		org.mockito.Mockito.when(repository.findAll()).thenReturn(List.of(running, succeeded, failed));
		org.mockito.Mockito.when(runner.getTaskRepository()).thenReturn(repository);
		org.mockito.Mockito.when(runner.newTask()).thenReturn((java.util.function.Supplier) TaskSampleNode::new);
		beanFactory().registerSingleton("otherRunner", runner);
		try {
			final var vo = runner(resource.findAll(), "otherRunner");
			Assertions.assertNotNull(vo);
			Assertions.assertEquals(TaskStatusType.OTHER, vo.getType());
			Assertions.assertEquals(3, vo.getStats().total());
			Assertions.assertEquals(1, vo.getStats().running());
			Assertions.assertEquals(1, vo.getStats().succeeded());
			Assertions.assertEquals(1, vo.getStats().failed());

			final var tasks = resource.findTasks("otherRunner", newUriInfo(), null);
			Assertions.assertEquals(3, tasks.getData().size());
			Assertions.assertEquals(TaskStatusType.OTHER, tasks.getData().getFirst().getLocked().getType());
		} finally {
			destroyRunner("otherRunner");
		}
	}

	@Test
	void comparators() {
		final var t1 = taskVo(1, "bob", 1000L, 4000L, TaskStatus.SUCCEEDED);
		final var t2 = taskVo(2, "Alice", 2000L, null, TaskStatus.RUNNING);

		// Single property comparators, unknown falls back to start date
		Assertions.assertTrue(resource.comparatorFor("id").compare(t1, t2) < 0);
		Assertions.assertTrue(resource.comparatorFor("author").compare(t1, t2) > 0);
		Assertions.assertTrue(resource.comparatorFor("end").compare(t1, t2) < 0);
		Assertions.assertTrue(resource.comparatorFor("status").compare(t1, t2) > 0);
		Assertions.assertTrue(resource.comparatorFor("unknown-property").compare(t1, t2) < 0);

		// Composed sort: ascending then descending secondary
		final var composed = resource.comparator(
				org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("author"),
						org.springframework.data.domain.Sort.Order.desc("id")));
		Assertions.assertTrue(composed.compare(t2, t1) < 0);

		// No sort: default is start date descending
		Assertions.assertTrue(resource.comparator(org.springframework.data.domain.Sort.unsorted()).compare(t2, t1) < 0);
	}

	private TaskVo taskVo(final int id, final String author, final Long start, final Long end, final TaskStatus status) {
		final var vo = new TaskVo();
		vo.setId(id);
		vo.setAuthor(author);
		vo.setStart(start == null ? null : new Date(start));
		vo.setEnd(end == null ? null : new Date(end));
		vo.setStatus(status);
		return vo;
	}

	@Test
	void statusJsonAndParse() {
		Assertions.assertEquals("failed", TaskStatus.FAILED.toJson());
		Assertions.assertEquals("other", TaskStatusType.OTHER.toJson());
		Assertions.assertNull(TaskStatus.parse(null));
		Assertions.assertNull(TaskStatus.parse("  "));
		Assertions.assertEquals(TaskStatus.RUNNING, TaskStatus.parse(" running "));
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

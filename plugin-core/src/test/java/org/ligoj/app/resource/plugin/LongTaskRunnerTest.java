package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.dao.TaskSampleNodeRepository;
import org.ligoj.app.dao.TaskSampleSubscriptionRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.model.TaskSampleNode;
import org.ligoj.app.model.TaskSampleSubscription;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.node.TaskSampleNodeResource;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.subscription.TaskSampleSubscriptionResource;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link LongTaskRunner}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class LongTaskRunnerTest extends AbstractOrgTest {

	protected TaskSampleSubscriptionResource resource;
	protected TaskSampleNodeResource resourceNode;

	@Autowired
	protected TaskSampleSubscriptionRepository repository;
	@Autowired
	protected TaskSampleNodeRepository repositoryNode;

	protected int subscription;

	@BeforeEach
	public void prepareSubscription() throws IOException {
		persistEntities("csv", new Class[] { Event.class, DelegateNode.class }, StandardCharsets.UTF_8.name());
		persistSystemEntities();
		this.subscription = getSubscription("MDA");
		this.resource = applicationContext.getAutowireCapableBeanFactory().createBean(TaskSampleSubscriptionResource.class);
		this.resourceNode = applicationContext.getAutowireCapableBeanFactory().createBean(TaskSampleNodeResource.class);
	}

	@Test
	public void cancel() {
		final TaskSampleNode task = newTaskSampleNode();
		task.setEnd(null);
		Assertions.assertFalse(task.isFailed());
		repositoryNode.saveAndFlush(task);
		resourceNode.cancel(task.getLocked().getId());
		Assertions.assertTrue(resourceNode.getTask(task.getLocked().getId()).isFailed());
	}

	@Test
	public void cancelNotRunnging() {
		final TaskSampleNode task = newTaskSampleNode();
		repositoryNode.saveAndFlush(task);
		Assertions.assertThrows(BusinessException.class, () -> {
			resourceNode.cancel(task.getLocked().getId());
		});
	}

	@Test
	public void deleteTaskNoTask() {
		resource.deleteTask(subscription);
	}

	@Test
	public void deleteTaskNotRunnging() {
		final TaskSampleSubscription taskSample = newTaskSample();
		repository.saveAndFlush(taskSample);
		em.flush();
		em.clear();
		resource.deleteTask(subscription);
		Assertions.assertEquals(0, repository.count());
	}

	@Test
	public void deleteTaskRunnging() {
		final TaskSampleSubscription taskSample = newTaskSample();
		taskSample.setEnd(null);
		repository.saveAndFlush(taskSample);
		em.flush();
		em.clear();
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.deleteTask(subscription);
		});
	}

	private TaskSampleSubscription newTaskSample() {
		final TaskSampleSubscription taskSample = new TaskSampleSubscription();
		taskSample.setAuthor(DEFAULT_USER);
		taskSample.setData("custom");
		taskSample.setStart(new Date());
		taskSample.setEnd(new Date());
		taskSample.setLocked(em.find(Subscription.class, subscription));
		return taskSample;
	}

	private TaskSampleNode newTaskSampleNode() {
		final TaskSampleNode taskSample = new TaskSampleNode();
		taskSample.setAuthor(DEFAULT_USER);
		taskSample.setData("custom");
		taskSample.setStart(new Date());
		taskSample.setEnd(new Date());
		taskSample.setLocked(em.find(Node.class, "service:bt:jira"));
		return taskSample;
	}

	@Test
	public void getTask() {
		repository.saveAndFlush(newTaskSample());
		final TaskSampleSubscription task = resource.getTask(subscription);
		assertTask(task);
		Assertions.assertNotNull(task.getEnd());
	}

	@Test
	public void endTask() {
		final TaskSampleSubscription newTaskSample = newTaskSample();
		newTaskSample.setEnd(null);
		repository.saveAndFlush(newTaskSample);
		resource.endTask(subscription, true);
		TaskSampleSubscription task = resource.getTask(subscription);
		assertTask(task);
		Assertions.assertTrue(task.isFailed());
		Assertions.assertNotNull(task.getEnd());
	}

	@Test
	public void endTaskAlreadyFinished() {
		repository.saveAndFlush(newTaskSample());
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.endTask(subscription, true);
		});
	}

	@Test
	public void startTask() {
		resource.startTask(subscription, task -> task.setData("init"));
		final TaskSampleSubscription task = resource.getTask(subscription);
		assertTask(task, "init");
		Assertions.assertFalse(task.isFailed());
		Assertions.assertNull(task.getEnd());
	}

	@Test
	public void startTaskNotRunning() {
		repository.saveAndFlush(newTaskSample());
		resource.startTask(subscription, task -> task.setData("init"));
		final TaskSampleSubscription task = resource.getTask(subscription);
		assertTask(task, "init");
		Assertions.assertNull(task.getEnd());
		Assertions.assertFalse(task.isFailed());
	}

	@Test
	public void nextStep() {
		final TaskSampleSubscription newTaskSample = newTaskSample();
		newTaskSample.setEnd(null);
		repository.saveAndFlush(newTaskSample);
		resource.nextStep(subscription, t -> t.setData("step2"));
		Assertions.assertEquals("step2", newTaskSample.getData());
	}

	@Test
	public void nextStepNotFound() {
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.nextStep(subscription, t -> t.setData("step2"));
		});
	}

	@Test
	public void nextStepAlreadyFinished() {
		repository.saveAndFlush(newTaskSample());
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.nextStep(subscription, t -> t.setData("step2"));
		});
	}

	/**
	 * There is already a running task on this subscription.
	 */
	@Test
	public void startTaskRunning() {
		final TaskSampleSubscription newTaskSample = newTaskSample();
		newTaskSample.setEnd(null);
		repository.saveAndFlush(newTaskSample);
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.startTask(subscription, task -> task.setData("init"));
		});
	}

	/**
	 * Task is locally finished, but not from the external system view.
	 */
	@Test
	public void startTaskRunningRemote() {
		resource = new TaskSampleSubscriptionResource() {

			@Override
			public boolean isFinished(final TaskSampleSubscription task) {
				// Never remotely finished
				return false;
			}
		};
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);

		final TaskSampleSubscription newTaskSample = newTaskSample();
		repository.saveAndFlush(newTaskSample);
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.startTask(subscription, task -> task.setData("init"));
		});
	}

	private void assertTask(TaskSampleSubscription task) {
		assertTask(task, "custom");
	}

	private void assertTask(TaskSampleSubscription task, final String data) {
		Assertions.assertEquals(DEFAULT_USER, task.getAuthor());
		Assertions.assertEquals(data, task.getData());
		Assertions.assertNotNull(task.getStart());
		Assertions.assertEquals(subscription, task.getLocked().getId().intValue());
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one
	 * subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}
}

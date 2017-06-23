package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.dao.TaskSampleRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.model.TaskSample;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.subscription.TaskSampleResource;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link LongTaskRunner}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class LongTaskRunnerTest extends AbstractOrgTest {

	protected TaskSampleResource resource;

	@Autowired
	protected TaskSampleRepository repository;

	protected int subscription;

	@Before
	public void prepareSubscription() throws IOException {
		persistEntities("csv", new Class[] { Event.class, DelegateNode.class }, StandardCharsets.UTF_8.name());
		persistSystemEntities();
		this.subscription = getSubscription("MDA");
		this.resource = new TaskSampleResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
	}

	@Test
	public void deleteTaskNoTask() {
		resource.deleteTask(subscription);
	}

	@Test
	public void deleteTaskNotRunnging() {
		final TaskSample taskSample = newTaskSample();
		repository.saveAndFlush(taskSample);
		em.flush();
		em.clear();
		resource.deleteTask(subscription);
		Assert.assertEquals(0, repository.count());
	}

	@Test(expected = BusinessException.class)
	public void deleteTaskRunnging() {
		final TaskSample taskSample = newTaskSample();
		taskSample.setEnd(null);
		repository.saveAndFlush(taskSample);
		em.flush();
		em.clear();
		resource.deleteTask(subscription);
	}

	private TaskSample newTaskSample() {
		final TaskSample taskSample = new TaskSample();
		taskSample.setAuthor(DEFAULT_USER);
		taskSample.setData("custom");
		taskSample.setStart(new Date());
		taskSample.setEnd(new Date());
		taskSample.setSubscription(em.find(Subscription.class, subscription));
		return taskSample;
	}

	@Test
	public void getTask() {
		repository.saveAndFlush(newTaskSample());
		final TaskSample task = resource.getTask(subscription);
		assertTask(task);
		Assert.assertNotNull(task.getEnd());
	}

	@Test
	public void endTask() {
		final TaskSample newTaskSample = newTaskSample();
		newTaskSample.setEnd(null);
		repository.saveAndFlush(newTaskSample());
		resource.endTask(subscription, true);
		TaskSample task = resource.getTask(subscription);
		assertTask(task);
		Assert.assertTrue(task.isFailed());
		Assert.assertNotNull(task.getEnd());
	}

	@Test
	public void startTask() {
		resource.startTask(subscription);
		final TaskSample task = resource.getTask(subscription);
		assertTask(task, "init");
		Assert.assertFalse(task.isFailed());
		Assert.assertNull(task.getEnd());
	}

	@Test
	public void startTaskNotRunning() {
		repository.saveAndFlush(newTaskSample());
		resource.startTask(subscription);
		final TaskSample task = resource.getTask(subscription);
		assertTask(task, "init");
		Assert.assertNull(task.getEnd());
		Assert.assertFalse(task.isFailed());
	}

	@Test
	public void nextStep() {
		final TaskSample newTaskSample = newTaskSample();
		newTaskSample.setEnd(null);
		repository.saveAndFlush(newTaskSample);
		resource.nextStep(newTaskSample);
		Assert.assertEquals("step2", newTaskSample.getData());
	}

	/**
	 * There is already a running task on this subscription.
	 */
	@Test(expected = BusinessException.class)
	public void startTaskRunning() {
		final TaskSample newTaskSample = newTaskSample();
		newTaskSample.setEnd(null);
		repository.saveAndFlush(newTaskSample);
		resource.startTask(subscription);
	}

	private void assertTask(TaskSample task) {
		assertTask(task, "custom");
	}

	private void assertTask(TaskSample task, final String data) {
		Assert.assertEquals(DEFAULT_USER, task.getAuthor());
		Assert.assertEquals(data, task.getData());
		Assert.assertNotNull(task.getStart());
		Assert.assertEquals(subscription, task.getSubscription().getId().intValue());
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one
	 * subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}
}

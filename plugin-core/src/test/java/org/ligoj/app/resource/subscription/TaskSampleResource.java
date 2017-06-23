package org.ligoj.app.resource.subscription;

import java.util.function.Supplier;

import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.TaskSampleRepository;
import org.ligoj.app.model.TaskSample;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

/**
 * An advanced mocked {@link LongTaskRunner} without transaction inner issue.
 */
public class TaskSampleResource implements ServicePlugin, LongTaskRunner<TaskSample, TaskSampleRepository> {

	@Autowired
	@Getter
	private TaskSampleRepository taskRepository;

	@Autowired
	@Getter
	private SubscriptionRepository subscriptionRepository;

	@Override
	public String getKey() {
		return "any";
	}

	@Override
	public Supplier<TaskSample> newTask() {
		return TaskSample::new;
	}

	@Override
	public void resetTask(TaskSample task) {
		task.setData("init");
	}

	@Override
	public void nextStep(final TaskSample task) {
		task.setData("step2");
		LongTaskRunner.super.nextStep(task);
	}

	@Override
	public TaskSample startTask(final int subscription) {
		return LongTaskRunner.super.startTask(subscription);
	}

	@Override
	public void endTask(final int subscription, final boolean failed) {
		LongTaskRunner.super.endTask(subscription, failed);
	}

}

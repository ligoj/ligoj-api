/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import lombok.Getter;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.TaskSampleSubscriptionRepository;
import org.ligoj.app.model.TaskSampleSubscription;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An advanced mocked {@link LongTaskRunner} without transaction inner issue.
 */
public class TaskSampleSubscriptionNoTxResource
		implements LongTaskRunnerSubscription<TaskSampleSubscription, TaskSampleSubscriptionRepository> {

	@Autowired
	@Getter
	private TaskSampleSubscriptionRepository taskRepository;

	@Autowired
	@Getter
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	@Getter
	private SubscriptionResource subscriptionResource;

	private final TaskSampleSubscription newTaskSample;

	public TaskSampleSubscriptionNoTxResource(final TaskSampleSubscription newTaskSample) {
		this.newTaskSample = newTaskSample;
	}

	@Override
	public Supplier<TaskSampleSubscription> newTask() {
		return TaskSampleSubscription::new;
	}

	@Override
	public TaskSampleSubscription nextStepInternal(final Integer subscription, final Consumer<TaskSampleSubscription> stepper) {
		// No transaction level access
		newTaskSample.setData("next");
		return newTaskSample;
	}

	@Override
	public TaskSampleSubscription startTaskInternal(final Integer subscription, final Consumer<TaskSampleSubscription> initializer) {
		// No transaction level access
		newTaskSample.setData("started");
		initializer.accept(newTaskSample);
		return newTaskSample;
	}

	@Override
	public TaskSampleSubscription endTaskInternal(final Integer subscription, final boolean failed, final Consumer<TaskSampleSubscription> finalizer) {
		// No transaction level access
		newTaskSample.setData("ended");
		newTaskSample.setEnd(new Date());
		finalizer.accept(newTaskSample);
		return newTaskSample;
	}

}

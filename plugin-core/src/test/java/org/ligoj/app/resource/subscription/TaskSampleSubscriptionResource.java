package org.ligoj.app.resource.subscription;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.TaskSampleSubscriptionRepository;
import org.ligoj.app.model.TaskSampleSubscription;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

/**
 * An advanced mocked {@link LongTaskRunner} without transaction inner issue.
 */
public class TaskSampleSubscriptionResource
		implements ServicePlugin, LongTaskRunnerSubscription<TaskSampleSubscription, TaskSampleSubscriptionRepository> {

	@Autowired
	@Getter
	private TaskSampleSubscriptionRepository taskRepository;

	@Autowired
	@Getter
	private SubscriptionRepository subscriptionRepository;

	@Override
	public String getKey() {
		return "any";
	}

	@Override
	public Supplier<TaskSampleSubscription> newTask() {
		return TaskSampleSubscription::new;
	}

	@Override
	public TaskSampleSubscription nextStep(final Integer subscription, final Consumer<TaskSampleSubscription> stepper) {
		return LongTaskRunnerSubscription.super.nextStep(subscription, stepper);
	}

	@Override
	public TaskSampleSubscription startTask(final Integer subscription, final Consumer<TaskSampleSubscription> initializer) {
		return LongTaskRunnerSubscription.super.startTask(subscription, initializer);
	}

	@Override
	public TaskSampleSubscription endTask(final Integer subscription, final boolean failed) {
		return LongTaskRunnerSubscription.super.endTask(subscription, failed);
	}

}

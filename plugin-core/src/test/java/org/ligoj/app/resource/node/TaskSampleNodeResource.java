package org.ligoj.app.resource.node;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.TaskSampleNodeRepository;
import org.ligoj.app.model.TaskSampleNode;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

/**
 * An advanced mocked {@link LongTaskRunner} without transaction inner issue.
 */
public class TaskSampleNodeResource implements ServicePlugin, LongTaskRunnerNode<TaskSampleNode, TaskSampleNodeRepository> {

	@Autowired
	@Getter
	private TaskSampleNodeRepository taskRepository;

	@Autowired
	@Getter
	private NodeRepository nodeRepository;

	@Override
	public String getKey() {
		return "any";
	}

	@Override
	public Supplier<TaskSampleNode> newTask() {
		return TaskSampleNode::new;
	}

	@Override
	public TaskSampleNode nextStep(final String node, final Consumer<TaskSampleNode> stepper) {
		return LongTaskRunnerNode.super.nextStep(node, stepper);
	}

	@Override
	public TaskSampleNode startTask(final String node, final Consumer<TaskSampleNode> initializer) {
		return LongTaskRunnerNode.super.startTask(node, initializer);
	}

	@Override
	public TaskSampleNode endTask(final String node, final boolean failed) {
		return LongTaskRunnerNode.super.endTask(node, failed);
	}

	@Override
	public TaskSampleNode cancel(final String node) {
		return LongTaskRunnerNode.super.cancel(node);
	}

}

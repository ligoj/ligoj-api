/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import lombok.Getter;
import lombok.Setter;

/**
 * Reference to the entity locked by a task, with just enough data for the UI to render it (a node icon, or a
 * subscription with its project link).
 */
@Getter
@Setter
public class LockedRefVo {

	/**
	 * Kind of locked reference.
	 */
	private TaskStatusType type;

	/**
	 * Node identifier. Set for a node task (the locked node) and for a subscription task (the subscription's service
	 * node, used for the icon). {@code null} otherwise.
	 */
	private String node;

	/**
	 * Subscription identifier. Set only for a subscription task.
	 */
	private Integer subscription;

	/**
	 * Owning project. Set only for a subscription task.
	 */
	private ProjectRef project;

	/**
	 * Minimal project reference.
	 *
	 * @param id   The project identifier.
	 * @param name The project name.
	 */
	public record ProjectRef(int id, String name) {
	}

	/**
	 * Build a node reference.
	 *
	 * @param node The locked node identifier.
	 * @return the reference.
	 */
	public static LockedRefVo ofNode(final String node) {
		final var ref = new LockedRefVo();
		ref.type = TaskStatusType.NODE;
		ref.node = node;
		return ref;
	}

	/**
	 * Build a subscription reference.
	 *
	 * @param subscription The locked subscription identifier.
	 * @param node         The subscription's service node identifier.
	 * @param projectId    The owning project identifier.
	 * @param projectName  The owning project name.
	 * @return the reference.
	 */
	public static LockedRefVo ofSubscription(final int subscription, final String node, final int projectId,
			final String projectName) {
		final var ref = new LockedRefVo();
		ref.type = TaskStatusType.SUBSCRIPTION;
		ref.subscription = subscription;
		ref.node = node;
		ref.project = new ProjectRef(projectId, projectName);
		return ref;
	}

	/**
	 * Build a reference for an unclassified runner.
	 *
	 * @return the reference.
	 */
	public static LockedRefVo ofOther() {
		final var ref = new LockedRefVo();
		ref.type = TaskStatusType.OTHER;
		return ref;
	}
}

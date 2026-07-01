/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kind of locked resource a {@link org.ligoj.app.resource.plugin.LongTaskRunner} operates on.
 */
public enum TaskStatusType {

	/**
	 * Runner locking a {@link org.ligoj.app.model.Node} (implements {@code LongTaskRunnerNode}).
	 */
	NODE,

	/**
	 * Runner locking a {@link org.ligoj.app.model.Subscription} (implements {@code LongTaskRunnerSubscription}).
	 */
	SUBSCRIPTION,

	/**
	 * Any other runner, not bound to a node nor a subscription.
	 */
	OTHER;

	/**
	 * Lower-case representation used in the JSON payload.
	 *
	 * @return the lower-case name.
	 */
	@JsonValue
	public String toJson() {
		return name().toLowerCase();
	}
}

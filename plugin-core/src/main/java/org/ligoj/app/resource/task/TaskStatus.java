/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Derived status of a {@link org.ligoj.app.model.AbstractLongTask}.
 * <ul>
 * <li>{@link #RUNNING}: not finished yet ({@code end} is {@code null}).</li>
 * <li>{@link #FAILED}: finished with the {@code failed} flag set.</li>
 * <li>{@link #SUCCEEDED}: finished without the {@code failed} flag.</li>
 * </ul>
 */
public enum TaskStatus {

	/**
	 * Task is still running ({@code end} is {@code null}).
	 */
	RUNNING,

	/**
	 * Task finished successfully.
	 */
	SUCCEEDED,

	/**
	 * Task finished with a failure.
	 */
	FAILED;

	/**
	 * Lower-case representation used in the JSON payload.
	 *
	 * @return the lower-case name.
	 */
	@JsonValue
	public String toJson() {
		return name().toLowerCase();
	}

	/**
	 * Parse a nullable/blank filter value into a status (case-insensitive).
	 *
	 * @param value the raw query value.
	 * @return the matching status, or {@code null} when blank.
	 */
	public static TaskStatus parse(final String value) {
		return value == null || value.isBlank() ? null : valueOf(value.trim().toUpperCase());
	}
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * A single task row: the scalar fields of {@link org.ligoj.app.model.AbstractLongTask} plus the locked entity
 * reference. Runner-specific fields are intentionally not exposed (they vary per runner).
 */
@Getter
@Setter
public class TaskVo {

	/**
	 * Task identifier.
	 */
	private Integer id;

	/**
	 * User who started the task.
	 */
	private String author;

	/**
	 * Start date.
	 */
	private Date start;

	/**
	 * End date, {@code null} while running.
	 */
	private Date end;

	/**
	 * Derived status.
	 */
	private TaskStatus status;

	/**
	 * Reference to the locked entity (node or subscription).
	 */
	private LockedRefVo locked;
}

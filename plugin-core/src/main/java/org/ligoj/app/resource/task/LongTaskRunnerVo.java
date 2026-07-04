/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Descriptor of a {@link org.ligoj.app.resource.plugin.LongTaskRunner} bean, with its visible task statistics.
 */
@Getter
@Setter
@AllArgsConstructor
public class LongTaskRunnerVo {

	/**
	 * Stable runner key: the Spring bean name. Used as path parameter to list its tasks.
	 */
	private String key;

	/**
	 * Human-readable label: the task entity simple class name (e.g. {@code ImportCatalogStatus}).
	 */
	private String label;

	/**
	 * Runner classification.
	 */
	private TaskStatusType type;

	/**
	 * Task counters by status, over the visible tasks of the current principal.
	 */
	private TaskStatsVo stats;
}

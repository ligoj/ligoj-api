/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import jakarta.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Sample long task model.
 */
@Entity
public class TaskSampleSubscription extends AbstractLongTaskSubscription {

	@Getter
	@Setter
	private String data;
}

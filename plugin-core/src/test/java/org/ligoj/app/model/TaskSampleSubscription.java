package org.ligoj.app.model;

import javax.persistence.Entity;

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

package org.ligoj.app.model;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Sample long task model.
 */
@Entity
public class TaskSampleSubscription extends AbstractLongTaskSubscription {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String data;
}

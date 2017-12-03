package org.ligoj.app.model;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Sample long task model.
 */
@Entity
public class TaskSampleNode extends AbstractLongTaskNode {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String data;
}

package org.ligoj.app.model;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Sample long task model.
 */
@Entity
public class TaskSampleNode extends AbstractLongTaskNode {

	@Getter
	@Setter
	private String data;
}

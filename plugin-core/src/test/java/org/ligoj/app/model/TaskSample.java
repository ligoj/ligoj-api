package org.ligoj.app.model;

import javax.persistence.Entity;

import org.ligoj.app.model.AbstractLongTask;

import lombok.Getter;
import lombok.Setter;

/**
 * Sample long task model.
 */
@Entity
public class TaskSample extends AbstractLongTask {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String data;
}

package org.ligoj.app.model;

import java.util.Date;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.ligoj.bootstrap.core.model.AbstractPersistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Import status. Is deleted only with the project.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractLongTask extends AbstractPersistable<Integer> {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	@NotNull
	@JsonIgnore
	@JoinColumn(name = "subscription")
	private Subscription subscription;

	@NotNull
	private Date start;

	/**
	 * Current status. <code>true</code> means failed.
	 */
	private boolean failed;

	/**
	 * Null while not finished.
	 * 
	 * @see #isFinished()
	 */
	private Date end;

	/**
	 * User proceeding the import.
	 */
	@NotNull
	private String author;

	/**
	 * Indicates the current task is finished.
	 */
	public boolean isFinished() {
		return end != null;
	}
}

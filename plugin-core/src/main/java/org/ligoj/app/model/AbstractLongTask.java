/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

import org.ligoj.bootstrap.core.model.AbstractPersistable;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Long task status. Is deleted with the project, the subscription or the related node hierarchy.
 *
 * @param <L> The locked type during while this task is running.
 * @param <I> The locked object 's identifier type during while this task is running.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractLongTask<L extends Persistable<I>, I extends Serializable>
		extends AbstractPersistable<Integer> {

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
	 *
	 * @return <code>true</code> when this task is finished.
	 */
	@JsonIgnore
	public boolean isFinished() {
		return end != null;
	}

	/**
	 * Return the locked entity while this task is running.
	 *
	 * @return the locked entity while this task is running.
	 */
	public abstract L getLocked();

	/**
	 * Set the locked entity while this task is running.
	 *
	 * @param locked the locked entity while this task is running.
	 */
	public abstract void setLocked(L locked);
}

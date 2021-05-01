/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Behavior of a node able to refine another element of the same type.
 *
 * @param <T> The current type
 */
public interface Refining<T> {

	/**
	 * The parent or refined element.
	 *
	 * @return The refined element. May be <code>null</code>.
	 */
	T getRefined();

	/**
	 * Indicate the current object is refining another object.
	 *
	 * @return <code>true</code> when the current object is refining another object.
	 */
	@JsonIgnore
	@Transient
	default boolean isRefining() {
		return getRefined() != null;
	}
}

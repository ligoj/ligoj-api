/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import jakarta.persistence.Transient;

import org.ligoj.app.model.Node;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Flag a class related to a {@link Node}.
 * @param <K>
 *            The primary key's type
 */
public interface NodeScoped<K> {

	/**
	 * Return the related node.
	 *
	 * @return The related node.
	 */
	@Transient
	Node getNode();

	/**
	 * Returns the id of the entity.
	 *
	 * @return the id. Can be {@literal null}.
	 */
	@Nullable
	K getId();

	/**
	 * Return <code>true</code> when this node is a service level.
	 *
	 * @return <code>true</code> when this node is a service level.
	 */
	@JsonIgnore
	default boolean isService() {
		return getNode().getId().split(":").length == 2;
	}

	/**
	 * Return <code>true</code> when this node is a service level.
	 *
	 * @return <code>true</code> when this node is a service level.
	 */
	@JsonIgnore
	default boolean isTool() {
		return getNode().getId().split(":").length == 3;
	}

	/**
	 * Return <code>true</code> when this node is a node/instance level.
	 *
	 * @return <code>true</code> when this node is a node/instance level.
	 */
	@JsonIgnore
	default boolean isInstance() {
		return getNode().getId().split(":").length == 4;
	}

	/**
	 * Return the tool instance if is an instance.
	 *
	 * @return The tool instance if is an instance.
	 */
	@JsonIgnore
	default Node getTool() {
		if (isService()) {
			return null;
		}
		return isInstance() ? getNode().getRefined() : getNode();
	}
}

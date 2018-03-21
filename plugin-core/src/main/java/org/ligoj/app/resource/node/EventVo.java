/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.LinkedHashSet;
import java.util.Set;

import org.ligoj.app.api.NodeVo;
import org.ligoj.app.model.EventType;

import lombok.Getter;
import lombok.Setter;

/**
 * Event's details.
 */
@Getter
@Setter
public class EventVo {

	/**
	 * Specific node events
	 */
	private final Set<EventVo> specifics = new LinkedHashSet<>();

	/**
	 * Value
	 */
	private String value;

	/**
	 * Type of event : status, ...
	 */
	private EventType type;

	/**
	 * Optional linked node providing this value. Either this attribute, either {@link #subscription} is not
	 * <code>null</code>.
	 */
	private NodeVo node;

	/**
	 * Optional linked subscription providing this value. Either this attribute, either {@link #node} is not
	 * <code>null</code>.
	 */
	private Integer subscription;

}

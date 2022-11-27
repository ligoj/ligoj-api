/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.ligoj.bootstrap.core.NamedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Node's details with little information.
 */
@Getter
@Setter
public abstract class AbstractNodeVo extends NamedBean<String> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Optional tag name. May be <code>null</code>.
	 */
	private String tag;

	/**
	 * Optional CSS classes used to render the tag. May be <code>null</code>.
	 */
	private String tagUiClasses;
}

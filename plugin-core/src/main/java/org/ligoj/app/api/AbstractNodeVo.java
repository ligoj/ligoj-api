package org.ligoj.app.api;

import org.ligoj.bootstrap.core.NamedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Node's details with few information.
 */
@Getter
@Setter
public abstract class AbstractNodeVo extends NamedBean<String> {
	
	/**
	 * Optional tag name. May be <code>null</code>.
	 */
	private String tag;
	
	/**
	 * Optional CSS classes used to render the tag. May be <code>null</code>.
	 */
	private String tagUiClasses;
}

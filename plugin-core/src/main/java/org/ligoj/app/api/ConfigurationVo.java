/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.util.Map;

import org.ligoj.bootstrap.core.IDescribableBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Extra configuration for a plug-in and attached to a subscription.
 */
@Getter
@Setter
public class ConfigurationVo {

	/**
	 * Attached subscription.
	 */
	private int subscription;

	/**
	 * Attached project.
	 */
	private IDescribableBean<Integer> project;

	/**
	 * Non secured parameters attached to this subscription.
	 */
	private Map<String, String> parameters;

	/**
	 * Subscribed service.
	 */
	private NodeVo node;

	/**
	 * Tool specific configuration. May be <code>null</code>.
	 */
	private Object configuration;

}

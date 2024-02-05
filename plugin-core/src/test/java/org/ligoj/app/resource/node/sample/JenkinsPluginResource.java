/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * Jenkins resource.
 */
@Component
public class JenkinsPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = BuildResource.SERVICE_URL + "/jenkins";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);


	@Override
	public String getKey() {
		return KEY;
	}

}

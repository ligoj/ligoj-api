/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * Sonar resource.
 */
@Component
public class SonarPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = KpiResource.SERVICE_URL + "/sonar";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Sonar user name able to connect to instance.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * Sonar user password able to connect to instance.
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	/**
	 * Sonar project's identifier, an integer
	 */
	public static final String PARAMETER_PROJECT = KEY + ":project";

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	@Override
	public String getKey() {
		return KEY;
	}

}

package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * Sonar resource.
 */
@Component
public class FortifyPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = SecurityResource.SERVICE_URL + "/fortify";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	/**
	 * Fortify project identifier.
	 */
	public static final String PARAMETER_KEY = KEY + ":pkey";

	/**
	 * Fortify project-version identifier.
	 */
	public static final String PARAMETER_VERSION = KEY + ":version";

	/**
	 * Fortify user name able to perform index.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * Fortify user password able to perform index.
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	@Override
	public String getKey() {
		return KEY;
	}

}

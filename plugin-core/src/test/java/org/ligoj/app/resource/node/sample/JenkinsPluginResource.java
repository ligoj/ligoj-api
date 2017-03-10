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

	/**
	 * Jenkins user name able to connect to instance.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * Jenkins user api-token able to connect to instance.
	 */
	public static final String PARAMETER_TOKEN = KEY + ":api-token";

	/**
	 * Jenkins job's name.
	 */
	public static final String PARAMETER_JOB = KEY + ":job";

	/**
	 * Jenkins job's name.
	 */
	public static final String PARAMETER_TEMPLATE_JOB = KEY + ":template-job";

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	@Override
	public String getKey() {
		return KEY;
	}

}

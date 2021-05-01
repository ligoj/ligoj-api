/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * vCloud VM resource.
 */
@Component
public class VCloudPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = VmResource.SERVICE_URL + "/vcloud";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * vCloud API base URL.
	 *
	 * @see "https://flexible-computing-advanced.sample.com/api"
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	/**
	 * vCloud user name.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * vCloud password able to perform VM operations.
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	/**
	 * vCloud organization.
	 */
	public static final String PARAMETER_ORGANIZATION = KEY + ":organization";

	/**
	 * The managed VM identifier.
	 */
	public static final String PARAMETER_VM = KEY + ":id";

	@Override
	public String getKey() {
		return KEY;
	}

}

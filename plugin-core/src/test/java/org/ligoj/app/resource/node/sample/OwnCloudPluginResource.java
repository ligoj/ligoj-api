/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * OwnCloud resource. Also see "index.php/apps/files/ajax/list.php" : My files
 */
@Component
public class OwnCloudPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = StorageResource.SERVICE_URL + "/owncloud";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * OwnCloud username able to connect to instance.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * OwnCloud user password able to connect to instance.
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	/**
	 * OwnCloud project's identifier, an integer
	 */
	public static final String PARAMETER_DIRECTORY = KEY + ":directory";

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	@Override
	public String getKey() {
		return KEY;
	}

}

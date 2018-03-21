/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * Confluence KM resource.
 * 
 * @see "https://docs.atlassian.com/atlassian-confluence/REST/latest"
 */
@Component
public class ConfluencePluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = KmResource.SERVICE_URL + "/confluence";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	/**
	 * Confluence space KEY (not name).
	 */
	public static final String PARAMETER_SPACE = KEY + ":space";

	/**
	 * Confluence user name able to perform index.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * Confluence user password able to perform index.
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	@Override
	public String getKey() {
		return KEY;
	}

}

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

	@Override
	public String getKey() {
		return KEY;
	}

}

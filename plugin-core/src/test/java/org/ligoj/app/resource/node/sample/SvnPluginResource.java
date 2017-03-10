package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * Subversion resource.
 */
@Component
public class SvnPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = ScmResource.SERVICE_URL + "/svn";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);


	@Override
	public String getKey() {
		return KEY;
	}
}

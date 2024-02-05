/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.ligoj.app.resource.plugin.AbstractServicePlugin;
import org.springframework.stereotype.Component;

/**
 * The Source Code Management service.
 */
@Component
public class ScmResource extends AbstractServicePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_URL = BASE_URL + "/scm";

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_KEY = SERVICE_URL.replace('/', ':').substring(1);

	@Override
	public String getKey() {
		return SERVICE_KEY;
	}

}

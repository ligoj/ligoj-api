package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

import org.ligoj.app.resource.plugin.AbstractServicePlugin;



/**
 * The knowledge management service.
 */
@Component
public class KmResource extends AbstractServicePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_URL = BASE_URL + "/km";

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_KEY = SERVICE_URL.replace('/', ':').substring(1);

	@Override
	public String getKey() {
		return SERVICE_KEY;
	}

}

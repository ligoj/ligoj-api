package org.ligoj.app.resource.node.sample;

import javax.ws.rs.PathParam;

import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.resource.plugin.AbstractServicePlugin;
import org.springframework.stereotype.Component;


/**
 * The Virtual Machine service.
 */
@Component
public class VmResource extends AbstractServicePlugin implements ConfigurablePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_URL = BASE_URL + "/vm";

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_KEY = SERVICE_URL.replace('/', ':').substring(1);

	@Override
	public String getKey() {
		return SERVICE_KEY;
	}

	@Override
	public Object getConfiguration(@PathParam("subscription") final int subscription) {
		return 0;
	}

}

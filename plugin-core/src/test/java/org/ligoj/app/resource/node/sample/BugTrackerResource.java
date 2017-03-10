package org.ligoj.app.resource.node.sample;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.resource.plugin.AbstractServicePlugin;

/**
 * The bug tracker service.
 */
@Component
public class BugTrackerResource extends AbstractServicePlugin implements ConfigurablePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_URL = BASE_URL + "/bt";

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_KEY = SERVICE_URL.replace('/', ':').substring(1);

	@Override
	@Transactional(value = TxType.SUPPORTS)
	public String getKey() {
		return SERVICE_KEY;
	}

	@Override
	public Object getConfiguration(final int subscription) throws Exception {
		return 0;
	}

}

package org.ligoj.app.resource.plugin;

import org.springframework.beans.factory.annotation.Autowired;

import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.SubscriptionRepository;

/**
 * Base implementation of {@link ServicePlugin} without action.
 */
public abstract class AbstractServicePlugin implements ServicePlugin {

	/**
	 * Root service URL
	 */
	public static final String BASE_URL = "/service";

	@Autowired
	protected SubscriptionRepository subscriptionRepository;

	/**
	 * Plug-in key.
	 */
	public static final String BASE_KEY = BASE_URL.replace('/', ':').substring(1);

	@Override
	public void delete(final int subscription, final boolean deleteRemoteData) throws Exception {
		// No custom data with this service
	}

	@Override
	public void create(final int subscription) {
		// No custom data with this service
	}

	@Override
	public void link(final int subscription) {
		// No custom data with this service
	}

}

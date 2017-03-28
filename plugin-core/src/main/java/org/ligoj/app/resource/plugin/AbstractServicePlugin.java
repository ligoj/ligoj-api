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

}

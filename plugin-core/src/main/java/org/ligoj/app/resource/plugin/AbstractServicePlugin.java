package org.ligoj.app.resource.plugin;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.Node;

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
	public List<Class<?>> getInstalledEntities() {
		return Collections.singletonList(Node.class);
	}

}

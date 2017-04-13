package org.ligoj.app.resource.node.sample;

import java.util.Map;

import org.ligoj.app.api.SubscriptionStatusWithData;

/**
 * Sonar resource.
 */
public abstract class AbstractToolPluginResource extends org.ligoj.app.resource.plugin.AbstractToolPluginResource {

	@Override
	public String getVersion(final Map<String, String> parameters) throws Exception {
		return "1";
	}

	@Override
	public String getLastVersion() {
		return "1";
	}

	@Override
	public boolean checkStatus(final Map<String, String> parameters) throws Exception {
		return true;
	}

	@Override
	public SubscriptionStatusWithData checkSubscriptionStatus(final Map<String, String> parameters)
			throws Exception {
		final SubscriptionStatusWithData status = new SubscriptionStatusWithData();
		status.put("property", "value");
		return status;
	}

	@Override
	public void link(final int subscription) throws Exception {
		// Validate the project key
	}
}

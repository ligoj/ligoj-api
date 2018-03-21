/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import java.util.Map;

import org.ligoj.app.api.SubscriptionStatusWithData;

/**
 * Sonar resource.
 */
public abstract class AbstractToolPluginResource extends org.ligoj.app.resource.plugin.AbstractToolPluginResource {

	@Override
	public String getVersion(final Map<String, String> parameters) {
		return "1";
	}

	@Override
	public String getLastVersion() {
		return "1";
	}

	@Override
	public boolean checkStatus(final Map<String, String> parameters) {
		return true;
	}

	@Override
	public SubscriptionStatusWithData checkSubscriptionStatus(final Map<String, String> parameters) {
		final SubscriptionStatusWithData status = new SubscriptionStatusWithData();
		status.put("property", "value");
		return status;
	}

	@Override
	public void link(final int subscription) {
		// Validate the project key
	}
}

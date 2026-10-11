/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import org.ligoj.bootstrap.resource.system.session.ISessionSettingsProvider;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.springframework.stereotype.Component;

/**
 * Tell the UI this API checks a node configuration before it is saved (<code>POST rest/node/status/check</code>):
 * without it, the node dialog does not offer the connection test, which an older API would answer with a 404.
 */
@Component
public class NodeCheckFeature implements ISessionSettingsProvider {

	/**
	 * Application data telling the UI the connection test is available.
	 */
	static final String FEATURE = "node-connection-check";

	@Override
	public void decorate(final SessionSettings settings) {
		settings.getApplicationSettings().getData().putIfAbsent(FEATURE, "true");
	}
}

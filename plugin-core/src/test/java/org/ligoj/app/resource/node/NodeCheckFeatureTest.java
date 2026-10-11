/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.resource.system.session.ApplicationSettings;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class of {@link NodeCheckFeature}: the UI offers the connection test only with an API having it.
 */
class NodeCheckFeatureTest {

	@Test
	void decorate() {
		final var settings = new SessionSettings();
		settings.setUserSettings(new HashMap<>());
		// Injected by Spring in a session: no setter
		ReflectionTestUtils.setField(settings, "applicationSettings", new ApplicationSettings());
		new NodeCheckFeature().decorate(settings);
		Assertions.assertEquals("true", settings.getApplicationSettings().getData().get("node-connection-check"));
	}
}

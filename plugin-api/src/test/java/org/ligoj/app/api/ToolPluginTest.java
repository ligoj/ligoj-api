/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link ToolPlugin}
 */
class ToolPluginTest {

	private final ToolPlugin plugin = () -> "service:s1:t2";

	@Test
	void checkStatus() throws Exception {
		Assertions.assertTrue(plugin.checkStatus(null));
	}

	@Test
	void checkStatusNode() throws Exception {
		Assertions.assertTrue(plugin.checkStatus(null, null));
	}

	@Test
	void checkSubscriptionStatus() throws Exception {
		final var data = plugin.checkSubscriptionStatus(0, null, null);
		data.put("some", "value");
		Assertions.assertTrue(data.getStatus().isUp());
		Assertions.assertEquals(1, data.getData().size());
		Assertions.assertEquals("value", data.getData().get("some"));
	}

	@Test
	void getLastVersion() throws Exception {
		Assertions.assertNull(plugin.getLastVersion());
	}

	@Test
	void getVersion() throws Exception {
		Assertions.assertNull(plugin.getVersion(null));
	}

	@Test
	void getCredentialExpiry() throws Exception {
		// Not implemented: an unknown expiry, the stored one is kept. Only JDK types, so a plug-in implementing it links
		// on an API without this method.
		final java.time.Instant expiry = plugin.getCredentialExpiry("service:s1:t2:i3", java.util.Map.of());
		Assertions.assertNull(expiry);
	}

	@Test
	void checkConnection() throws Exception {
		// By default, the anonymous status check of the given parameters, whatever the node
		final var parameters = Map.of("service:s1:t2:url", "http://localhost");
		final var checked = new java.util.ArrayList<Map<String, String>>();
		final ToolPlugin tool = new ToolPlugin() {
			@Override
			public String getKey() {
				return "service:s1:t2";
			}

			@Override
			public boolean checkStatus(final Map<String, String> given) {
				checked.add(given);
				return false;
			}
		};
		Assertions.assertFalse(tool.checkConnection("service:s1:t2:new", parameters));
		Assertions.assertEquals(List.of(parameters), checked);
	}
}

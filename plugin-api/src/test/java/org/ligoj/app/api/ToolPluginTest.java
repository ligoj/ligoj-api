/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link ToolPlugin}
 */
class ToolPluginTest {

	private final ToolPlugin plugin = new ToolPlugin() {

		@Override
		public String getKey() {
			return "service:s1:t2";
		}

	};

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
		final SubscriptionStatusWithData data = plugin.checkSubscriptionStatus(0, null, null);
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
}

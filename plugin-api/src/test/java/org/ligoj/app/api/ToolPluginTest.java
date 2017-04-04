package org.ligoj.app.api;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link ToolPlugin}
 */
public class ToolPluginTest {

	private final ToolPlugin plugin = new ToolPlugin() {

		@Override
		public String getKey() {
			return "service:s1:t2";
		}

		@Override
		public boolean checkStatus(String node, Map<String, String> parameters) throws Exception {
			return true;
		}

		@Override
		public SubscriptionStatusWithData checkSubscriptionStatus(String node, Map<String, String> parameters) throws Exception {
			final SubscriptionStatusWithData data = new SubscriptionStatusWithData(true);
			data.put("some", "value");
			return data;
		}
	};

	@Test
	public void checkStatus() throws Exception {
		Assert.assertTrue(plugin.checkStatus(null));
	}

	@Test
	public void checkStatusNode() throws Exception {
		Assert.assertTrue(plugin.checkStatus(null, null));
	}

	@Test
	public void checkSubscriptionStatus() throws Exception {
		final SubscriptionStatusWithData data = plugin.checkSubscriptionStatus(null);
		Assert.assertNotNull(data.getStatus().isUp());
		Assert.assertEquals("value", data.getData().get("some"));
	}

	@Test
	public void getLastVersion() throws Exception {
		Assert.assertNull(plugin.getLastVersion());
	}

	@Test
	public void getVersion() throws Exception {
		Assert.assertNull(plugin.getVersion(null));
	}
}

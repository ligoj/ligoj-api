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
		public String getVersion(Map<String, String> parameters) throws Exception {
			return "2.0.0";
		}

		@Override
		public String getLastVersion() throws Exception {
			return "1.0.0";
		}

		@Override
		public boolean checkStatus(String node, Map<String, String> parameters) throws Exception {
			return true;
		}

		@Override
		public SubscriptionStatusWithData checkSubscriptionStatus(String node, Map<String, String> parameters) throws Exception {
			return new SubscriptionStatusWithData();
		}
	};
	
	@Test
	public void testCheckStatus() throws Exception {
		Assert.assertTrue(plugin.checkStatus(null,null));
	}
}

package org.ligoj.app.api;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link FeaturePlugin}
 */
public class FeaturePluginTest {

	private final FeaturePlugin plugin = new FeaturePlugin() {

		@Override
		public String getKey() {
			return "service:s1:t2";
		}
	};

	@Test
	public void getName() throws Exception {
		Assert.assertEquals("T2", plugin.getName());
	}

	@Test
	public void getVendor() throws Exception {
		Assert.assertNull(plugin.getVendor());
	}

	@Test
	public void getVersion() throws Exception {
		Assert.assertNull(plugin.getVersion());
	}

	@Test
	public void getInstalledEntities() {
		Assert.assertTrue(plugin.getInstalledEntities().isEmpty());
	}

	@Test
	public void install() throws Exception {
		// Nothing done there
		plugin.install();
	}

	@Test
	public void update() {
		// Nothing done there
		plugin.update("any");
	}

	@Test
	public void compareTo() throws Exception {
		Assert.assertEquals(0, plugin.compareTo(plugin));
	}
}

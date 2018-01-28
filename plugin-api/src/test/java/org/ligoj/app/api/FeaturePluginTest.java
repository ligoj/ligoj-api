package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
		Assertions.assertEquals("T2", plugin.getName());
	}

	@Test
	public void getVendor() throws Exception {
		Assertions.assertNull(plugin.getVendor());
	}

	@Test
	public void getVersion() throws Exception {
		Assertions.assertNull(plugin.getVersion());
	}

	@Test
	public void getInstalledEntities() {
		Assertions.assertTrue(plugin.getInstalledEntities().isEmpty());
	}

	@Test
	public void install() throws Exception {
		// Nothing done there
		plugin.install();
	}

	@Test
	public void update() throws Exception {
		// Nothing done there
		plugin.update("any");
	}

	@Test
	public void compareTo() throws Exception {
		Assertions.assertEquals(0, plugin.compareTo(plugin));
	}
}

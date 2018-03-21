/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
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
	public void getName() {
		Assertions.assertEquals("T2", plugin.getName());
	}

	@Test
	public void getVendor() {
		Assertions.assertNull(plugin.getVendor());
	}

	@Test
	public void getVersion() {
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
	public void compareTo() {
		Assertions.assertEquals(0, plugin.compareTo(plugin));
	}
}

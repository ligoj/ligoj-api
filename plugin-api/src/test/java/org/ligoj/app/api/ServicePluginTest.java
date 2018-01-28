package org.ligoj.app.api;

import org.junit.jupiter.api.Test;

/**
 * Test class of {@link ServicePlugin}
 */
public class ServicePluginTest {

	private final ServicePlugin plugin = new ServicePlugin() {

		@Override
		public String getKey() {
			return "service:s1:t2";
		}
	};

	@Test
	public void delete() throws Exception {
		plugin.delete(2, true);
	}

	@Test
	public void create() throws Exception {
		plugin.create(1);
	}

	@Test
	public void link() throws Exception {
		plugin.link(3);
	}
}

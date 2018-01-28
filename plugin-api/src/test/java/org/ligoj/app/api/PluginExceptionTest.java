package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link PluginException}
 */
public class PluginExceptionTest {

	@Test
	public void getPlugin() {
		PluginException exception = new PluginException("some", "message");
		Assertions.assertEquals("some", exception.getPlugin());
		Assertions.assertEquals("Plugin issue for some:message", exception.getMessage());
	}

	@Test
	public void getPluginNotFound() {
		PluginException exception = new PluginNotFoundException("some");
		Assertions.assertEquals("some", exception.getPlugin());
		Assertions.assertEquals("Plugin issue for some:Not found", exception.getMessage());
	}
	
}

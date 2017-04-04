package org.ligoj.app.api;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link PluginException}
 */
public class PluginExceptionTest {

	@Test
	public void getPlugin() {
		PluginException exception = new PluginException("some", "message");
		Assert.assertEquals("some", exception.getPlugin());
		Assert.assertEquals("Plugin issue for some:message", exception.getMessage());
	}

	@Test
	public void getPluginNotFound() {
		PluginException exception = new PluginNotFoundException("some");
		Assert.assertEquals("some", exception.getPlugin());
		Assert.assertEquals("Plugin issue for some:Not found", exception.getMessage());
	}
	
}

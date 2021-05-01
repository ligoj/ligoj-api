/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link PluginException}
 */
class PluginExceptionTest {

	@Test
	void getPlugin() {
		var exception = new PluginException("some", "message");
		Assertions.assertEquals("some", exception.getPlugin());
		Assertions.assertEquals("Plugin issue for some:message", exception.getMessage());
	}

	@Test
	void getPluginNotFound() {
		PluginException exception = new PluginNotFoundException("some");
		Assertions.assertEquals("some", exception.getPlugin());
		Assertions.assertEquals("Plugin issue for some:Not found", exception.getMessage());
	}

}

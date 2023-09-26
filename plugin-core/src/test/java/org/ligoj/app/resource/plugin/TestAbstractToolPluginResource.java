/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class of {@link AbstractToolPluginResource}
 */
class TestAbstractToolPluginResource {

	private AbstractToolPluginResource resource;

	@BeforeEach
	void prepareMock() {
		resource = new AbstractToolPluginResource() {

			@Override
			public String getVersion(Map<String, String> parameters) {
				return "1.0.0";
			}

			@Override
			public String getKey() {
				return null;
			}
		};
	}

	@Test
	void create() {
		Assertions.assertThrows(NotImplementedException.class, () -> resource.create(55));
	}

	@Test
	void delete() throws Exception {
		// Nothing happens
		resource.delete(55, false);
	}

	@Test
	void getVersion() throws Exception {
		// Return the version from the subscription parameters
		resource.subscriptionResource = Mockito.mock(SubscriptionResource.class);
		Mockito.when(resource.subscriptionResource.getParameters(55)).thenReturn(new HashMap<>());
		Assertions.assertEquals("1.0.0", resource.getVersion(0));
	}


	@Test
	void getParameter() {
		resource.configuration = Mockito.mock(ConfigurationResource.class);
		Mockito.when(resource.configuration.get("param", "value-def")).thenReturn("value-conf");
		Mockito.when(resource.configuration.get("param2", "value-def")).thenReturn("value-def");
		Assertions.assertEquals("value-node", resource.getParameter(Map.of("param", "value-node"), "param", "value-def"));
		Assertions.assertEquals("value-conf", resource.getParameter(Collections.emptyMap(), "param", "value-def"));
		Assertions.assertEquals("value-def", resource.getParameter(Collections.emptyMap(), "param2", "value-def"));
	}

	@Test
	void download() {
		Assertions.assertNotNull(AbstractToolPluginResource.download(Mockito.mock(StreamingOutput.class), "file"));
	}

	@Test
	void getInstalledEntities() {
		Assertions.assertTrue(resource.getInstalledEntities().contains(Node.class));
		Assertions.assertTrue(resource.getInstalledEntities().contains(Parameter.class));
	}

	@Test
	void getInstalledEntitiesDefaultService() {
		Assertions.assertTrue(new AbstractServicePlugin() {

			@Override
			public String getKey() {
				return "key";
			}
		}.getInstalledEntities().contains(Node.class));
	}

	@Test
	void getInstalledEntitiesService() {
		Assertions.assertTrue(new ServicePlugin() {

			@Override
			public String getKey() {
				return "key";
			}
		}.getInstalledEntities().isEmpty());
	}

}

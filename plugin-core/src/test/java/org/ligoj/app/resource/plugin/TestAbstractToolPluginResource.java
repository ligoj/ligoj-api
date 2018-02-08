package org.ligoj.app.resource.plugin;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.mockito.Mockito;

/**
 * Test class of {@link AbstractToolPluginResource}
 */
public class TestAbstractToolPluginResource {

	private AbstractToolPluginResource resource;

	@BeforeEach
	public void prepareMock() {
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
	public void create() {
		Assertions.assertThrows(NotImplementedException.class, () -> {
			resource.create(55);
		});
	}

	@Test
	public void delete() throws Exception {
		// Nothing happens
		resource.delete(55, false);
	}

	@Test
	public void getVersion() throws Exception {
		// Return the version from the subscription parameters
		resource.subscriptionResource = Mockito.mock(SubscriptionResource.class);
		Mockito.when(resource.subscriptionResource.getParameters(55)).thenReturn(new HashMap<>());
		Assertions.assertEquals("1.0.0", resource.getVersion(0));
	}

	@Test
	public void download() {
		Assertions.assertNotNull(AbstractToolPluginResource.download(Mockito.mock(StreamingOutput.class), "file"));
	}

	@Test
	public void getInstalledEntities() {
		Assertions.assertTrue(resource.getInstalledEntities().contains(Node.class));
		Assertions.assertTrue(resource.getInstalledEntities().contains(Parameter.class));
	}

	@Test
	public void getInstalledEntitiesDefaultService() {
		Assertions.assertTrue(new AbstractServicePlugin() {

			@Override
			public String getKey() {
				return "key";
			}
		}.getInstalledEntities().contains(Node.class));
	}

	@Test
	public void getInstalledEntitiesService() {
		Assertions.assertTrue(new ServicePlugin() {

			@Override
			public String getKey() {
				return "key";
			}
		}.getInstalledEntities().isEmpty());
	}

}

package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.mockito.Mockito;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Test class of {@link AbstractXmlApiToolPluginResource}
 */
public class TestAbstractXmlApiToolPluginResource {

	private AbstractXmlApiToolPluginResource resource;

	@Before
	public void prepareMock() {
		resource = new AbstractXmlApiToolPluginResource() {

			@Override
			public String getVersion(Map<String, String> parameters) throws Exception {
				return "1.0.0";
			}

			@Override
			public boolean checkStatus(String node, Map<String, String> parameters) throws Exception {
				return false;
			}

			@Override
			public SubscriptionStatusWithData checkSubscriptionStatus(String node, Map<String, String> parameters) throws Exception {
				return null;
			}

			@Override
			public String getKey() {
				return null;
			}
		};
	}

	@Test
	public void getTags() throws SAXException, IOException, ParserConfigurationException {
		NodeList tags = resource.getTags("<root>any <tag1>value1</tag1> some <tag2>value2</tag2> some <tag1>value3</tag1></root>", "tag1");
		Assert.assertEquals(2, tags.getLength());
		Assert.assertEquals("tag1", tags.item(0).getNodeName());
		Assert.assertEquals("value1", tags.item(0).getTextContent());
		Assert.assertEquals("tag1", tags.item(1).getNodeName());
		Assert.assertEquals("value3", tags.item(1).getTextContent());
	}

	@Test
	public void getTagsLink() throws SAXException, IOException, ParserConfigurationException {
		NodeList tags = resource.getTags(null, "tag1");
		Assert.assertEquals(0, tags.getLength());
	}

	@Test(expected = NotImplementedException.class)
	public void create() throws Exception {
		resource.create(55);
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
		Assert.assertEquals("1.0.0", resource.getVersion(0));
	}

	@Test
	public void download() throws Exception {
		Assert.assertNotNull(AbstractToolPluginResource.download(Mockito.mock(StreamingOutput.class), "file"));
	}

	@Test
	public void getInstalledEntities() {
		Assert.assertTrue(resource.getInstalledEntities().contains(Node.class));
		Assert.assertTrue(resource.getInstalledEntities().contains(Parameter.class));
	}

	@Test
	public void getInstalledEntitiesDefaultService() {
		Assert.assertTrue(new AbstractServicePlugin() {

			@Override
			public String getKey() {
				return "key";
			}
		}.getInstalledEntities().contains(Node.class));
	}

	@Test
	public void getInstalledEntitiesService() {
		Assert.assertTrue(new ServicePlugin() {

			@Override
			public String getKey() {
				return "key";
			}
		}.getInstalledEntities().isEmpty());
	}

}

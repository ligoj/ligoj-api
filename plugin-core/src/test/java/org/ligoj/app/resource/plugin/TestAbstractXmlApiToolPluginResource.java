package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Test class of {@link AbstractXmlApiToolPluginResource}
 */
public class TestAbstractXmlApiToolPluginResource {

	private AbstractXmlApiToolPluginResource resource = new AbstractXmlApiToolPluginResource() {

		@Override
		public String getVersion(Map<String, String> parameters) throws Exception {
			return null;
		}

		@Override
		public String getLastVersion() throws Exception {
			return null;
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
		public void link(int subscription) throws Exception {
			// Nothing to do
		}

		@Override
		public String getKey() {
			return null;
		}
	};


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

}

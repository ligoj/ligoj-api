/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.model.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Test class of {@link XmlUtils}
 */
class XmlUtilsTest {

	private XmlUtils resource;

	@BeforeEach
	void prepareMock() {
		resource = new XmlUtils();
	}

	@Test
	void getTags() throws SAXException, IOException, ParserConfigurationException {
		final var tags = resource.getTags(
				"<root>any <tag1>value1</tag1> some <tag2>value2</tag2> some <tag1>value3</tag1></root>", "tag1");
		Assertions.assertEquals(2, tags.getLength());
		Assertions.assertEquals("tag1", tags.item(0).getNodeName());
		Assertions.assertEquals("value1", tags.item(0).getTextContent());
		Assertions.assertEquals("tag1", tags.item(1).getNodeName());
		Assertions.assertEquals("value3", tags.item(1).getTextContent());
	}

	@Test
	void getTagText() throws SAXException, IOException, ParserConfigurationException {
		final var jobsAsInput = IOUtils.toInputStream(
				"<root>any <tag1>value1</tag1> some <tag2>value2</tag2> some <tag3>  </tag3></root>",
				StandardCharsets.UTF_8);
		final var root = resource.parse(jobsAsInput);
		Assertions.assertEquals("value2", resource.getTagText((Element) root.getFirstChild(), "tag2"));
		Assertions.assertNull(resource.getTagText((Element) root.getFirstChild(), "tag3"));
	}

	@Test
	void getXpath() throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		Assertions.assertEquals(2,
				resource.getXpath(
						"<root>any <tag1>value1</tag1> some <tag2>value2</tag2> some <tag1>value3</tag1></root>",
						"root/tag1").getLength());
	}

	@Test
	void getTagsLink() throws SAXException, IOException, ParserConfigurationException {
		var tags = resource.getTags(null, "tag1");
		Assertions.assertEquals(0, tags.getLength());
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

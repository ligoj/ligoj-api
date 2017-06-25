package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML based API tool plug-in.
 */
public abstract class AbstractXmlApiToolPluginResource extends AbstractToolPluginResource {

	/**
	 * Build and return a secured document builder.
	 * 
	 * @param input
	 *            Input to parse.
	 */
	protected Document parse(final InputStream input) throws SAXException, IOException, ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setCoalescing(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return factory.newDocumentBuilder().parse(input, StandardCharsets.UTF_8.name());
	}

	/**
	 * Return list of tags inside the root element.
	 * 
	 * @param input
	 *            Input to parse. My be <code>null</code>.
	 * @param tag
	 *            The tags to return.
	 * @return Not <code>null</code> tag list.
	 */
	protected NodeList getTags(final String input, final String tag) throws SAXException, IOException, ParserConfigurationException {
		final InputStream jobsAsInput = IOUtils.toInputStream(ObjectUtils.defaultIfNull(input, "<a/>"), StandardCharsets.UTF_8);
		return ((Element) parse(jobsAsInput).getFirstChild()).getElementsByTagName(tag);
	}

}

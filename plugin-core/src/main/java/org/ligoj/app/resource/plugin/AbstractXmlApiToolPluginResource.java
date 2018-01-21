package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML based API tool plug-in.
 * 
 * @deprecated Use {@link XmlUtils} component instead.
 */
@Deprecated
public abstract class AbstractXmlApiToolPluginResource extends AbstractToolPluginResource {

	@Autowired
	protected XmlUtils xml;

	/**
	 * Build and return a secured document builder.
	 * 
	 * @param input
	 *            Input to parse.
	 */
	protected Document parse(final InputStream input) throws SAXException, IOException, ParserConfigurationException {
		return xml.parse(input);
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
		return xml.getTags(input, tag);
	}

	/**
	 * Return list of tags inside the root element from XPATH.
	 * 
	 * @param input
	 *            Input to parse. My be <code>null</code>.
	 * @param expression
	 *            The XPATH expression.
	 * @return Not <code>null</code> tag list.
	 */
	public NodeList getXpath(final String input, final String expression)
			throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		return xml.getTags(input, expression);
	}

	/**
	 * Return XML tag text content.
	 * 
	 * @param element
	 *            Optional element. Null is accepted.
	 * @param tag
	 *            The tag name.
	 * @return The tag value when tag is found of <code>null</code>.
	 */
	protected String getTagText(final Element element, final String tag) {
		return xml.getTagText(element, tag);
	}
}

/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML utilities.
 */
@Component
public class XmlUtils {

	/**
	 * The shared XPATH factory
	 */
	public final XPathFactory xpathFactory = XPathFactory.newInstance();

	/**
	 * Build and return a secured document builder.
	 * 
	 * @param input
	 *            Input to parse.
	 * @return The parsed document.
	 */
	public Document parse(final InputStream input) throws SAXException, IOException, ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setCoalescing(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return factory.newDocumentBuilder().parse(input, StandardCharsets.UTF_8.name());
	}

	/**
	 * Parse the given input and return the root element.
	 * 
	 * @param input
	 *            Input to parse. My be <code>null</code>.
	 * @return Not <code>null</code> root element.
	 */
	public Element parse(final String input) throws SAXException, IOException, ParserConfigurationException {
		final InputStream jobsAsInput = IOUtils.toInputStream(ObjectUtils.defaultIfNull(input, "<a/>"),
				StandardCharsets.UTF_8);
		return (Element) parse(jobsAsInput).getFirstChild();
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
	public NodeList getTags(final String input, final String tag)
			throws SAXException, IOException, ParserConfigurationException {
		return parse(input).getElementsByTagName(tag);
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
		final XPath xPath = xpathFactory.newXPath();
		return (NodeList) xPath.compile(expression).evaluate(
				parse(IOUtils.toInputStream(ObjectUtils.defaultIfNull(input, ""), StandardCharsets.UTF_8)),
				XPathConstants.NODESET);
	}

	/**
	 * Return XML tag text content. Empty content is considered as <code>null</code>.
	 * 
	 * @param element
	 *            Optional element. <code>null</code> is accepted.
	 * @param tag
	 *            The tag name.
	 * @return The trimmed tag value when tag is found of <code>null</code>.
	 */
	public String getTagText(final Element element, final String tag) {
		return Optional.ofNullable(Optional.ofNullable(element).map(e -> e.getElementsByTagName(tag).item(0))
				.map(Node::getTextContent).map(StringUtils::trimToNull).orElse(null)).orElse(null);
	}

}

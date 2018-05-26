package oakbot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents an XML document or an element in an XML document. This class acts
 * as a wrapper around Java's XML API to make it easier to interact with XML
 * documents.
 * @author Michael Angstadt
 */
public class Leaf {
	private final Node node;
	private final Element element;
	private final XPath xpath;

	/**
	 * Creates a new leaf.
	 * @param node the node to wrap (typically, a {@link Document} object)
	 */
	public Leaf(Node node) {
		this.node = node;
		this.element = (node instanceof Element) ? (Element) node : null;
		this.xpath = XPathFactory.newInstance().newXPath();
	}

	private Leaf(Element element, XPath xpath) {
		this.node = element;
		this.element = element;
		this.xpath = xpath;
	}

	/**
	 * Gets all child elements.
	 * @return the child elements
	 */
	public List<Leaf> children() {
		return leavesFrom(node.getChildNodes());
	}

	/**
	 * Selects the first element that matches the given xpath expression.
	 * @param expression the xpath expression
	 * @return the element or null if not found
	 */
	public Leaf selectFirst(String expression) {
		Element element;
		try {
			element = (Element) xpath.evaluate(expression, node, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			/*
			 * Since xpath expressions are almost always hard-coded, do not
			 * throw a checked exception.
			 */
			throw new RuntimeException(e);
		}

		return (element == null) ? null : new Leaf(element, xpath);
	}

	/**
	 * Selects the elements that match the given xpath expression.
	 * @param expression the xpath expression
	 * @return the elements
	 */
	public List<Leaf> select(String expression) {
		NodeList nodeList;
		try {
			nodeList = (NodeList) xpath.evaluate(expression, node, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			/*
			 * Since xpath expressions are almost always hard-coded, do not
			 * throw a checked exception.
			 */
			throw new RuntimeException(e);
		}

		return leavesFrom(nodeList);
	}

	/**
	 * Converts all the XML elements in the given node list to {@link Leaf}
	 * objects. All other types of XML nodes (e.g. comments, text, etc) are
	 * ignored.
	 * @param nodeList the node list
	 * @return the leaf objects
	 */
	private List<Leaf> leavesFrom(NodeList nodeList) {
		List<Leaf> leaves = new ArrayList<Leaf>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element) {
				Element element = (Element) node;
				Leaf leaf = new Leaf(element, xpath);
				leaves.add(leaf);
			}
		}
		return leaves;
	}

	/**
	 * Gets the value of an attribute.
	 * @param name the attribute name
	 * @return the attribute value or empty string if the attribute does not
	 * exist
	 */
	public String attribute(String name) {
		return element.getAttribute(name);
	}

	/**
	 * Gets all of the attributes.
	 * @return the attributes
	 */
	public Map<String, String> attributes() {
		NamedNodeMap attributes = element.getAttributes();
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attribute = (Attr) attributes.item(i);
			String key = attribute.getName();
			String value = attribute.getValue();
			map.put(key, value);
		}
		return map;
	}

	/**
	 * Gets the text content of the element.
	 * @return the text content
	 */
	public String text() {
		return node.getTextContent();
	}

	/**
	 * Gets the tag name.
	 * @return the tag name
	 */
	public String name() {
		return node.getNodeName();
	}

	/**
	 * Gets the wrapped node object.
	 * @return the wrapped node object
	 */
	public Node node() {
		return node;
	}
}

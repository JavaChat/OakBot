package oakbot.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wraps an XML {@link Node} object, providing utility functionality.
 * @author Michael Angstadt
 */
public class DocumentWrapper {
	private final Node root;
	private final XPath xpath;

	/**
	 * @param root the node to wrap
	 */
	public DocumentWrapper(Node root) {
		this.root = root;
		xpath = XPathFactory.newInstance().newXPath();
	}

	/**
	 * Queries the DOM for a specific element.
	 * @param query the xpath query
	 * @return the element or null if not found
	 */
	public Element element(String query) {
		return element(query, root);
	}

	/**
	 * Queries the DOM for a specific element.
	 * @param query the xpath query
	 * @param from the node to look inside of
	 * @return the element or null if not found
	 */
	public Element element(String query, Node from) {
		try {
			return (Element) xpath.evaluate(query, from, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Queries the DOM for multiple elements.
	 * @param query the xpath query
	 * @return the elements
	 */
	public List<Element> elements(String query) {
		return elements(query, root);
	}

	/**
	 * Queries the DOM for multiple elements.
	 * @param query the xpath query
	 * @param from the node to look inside of
	 * @return the elements
	 */
	public List<Element> elements(String query, Node from) {
		NodeList list;
		try {
			list = (NodeList) xpath.evaluate(query, from, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}

		List<Element> elements = new ArrayList<>(list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			elements.add((Element) list.item(i));
		}
		return elements;
	}

	@Override
	public String toString() {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			StringWriter sw = new StringWriter();
			DOMSource source = new DOMSource(root);
			StreamResult result = new StreamResult(sw);
			transformer.transform(source, result);
			return sw.toString();
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}
}

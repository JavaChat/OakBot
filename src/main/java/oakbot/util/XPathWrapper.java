package oakbot.util;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wraps an {@link XPath} object, providing convenience methods.
 * @author Michael Angstadt
 */
public class XPathWrapper {
	private final XPath xpath;

	public XPathWrapper() {
		this(XPathFactory.newInstance().newXPath());
	}

	/**
	 * @param xpath the XPath object to wrap
	 */
	public XPathWrapper(XPath xpath) {
		this.xpath = xpath;
	}

	/**
	 * Evaluates an expression.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @param returnType the return type
	 * @return the return value
	 */
	public Object eval(String expression, Object node, QName returnType) {
		try {
			return xpath.evaluate(expression, node, returnType);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Evaluates an expression, returning a string.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @return the string
	 */
	public String string(String expression, Object node) {
		return (String) eval(expression, node, XPathConstants.STRING);
	}

	/**
	 * Evaluates an expression, returning a {@link Node}.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @return the node or null if not found
	 */
	public Node node(String expression, Object node) {
		return (Node) eval(expression, node, XPathConstants.NODE);
	}

	/**
	 * Evaluates an expression, returning a {@link NodeList}.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @return the node list
	 */
	public Iterable<Node> nodelist(String expression, Object node) {
		return it((NodeList) eval(expression, node, XPathConstants.NODESET));
	}

	/**
	 * Returns the enclosed {@link XPath} object.
	 * @return the xpath object
	 */
	public XPath getXPath() {
		return xpath;
	}

	/**
	 * Allows {@link NodeList} objects to be used in foreach loops.
	 * @param list the node list
	 * @return the iterable instance
	 */
	public static Iterable<Node> it(final NodeList list) {
		final Iterator<Node> it = new Iterator<Node>() {
			private int cur = 0;

			@Override
			public boolean hasNext() {
				return cur < list.getLength();
			}

			@Override
			public Node next() {
				return list.item(cur++);
			}
		};

		return new Iterable<Node>() {
			@Override
			public Iterator<Node> iterator() {
				return it;
			}
		};
	}
}
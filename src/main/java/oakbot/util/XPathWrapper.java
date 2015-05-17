package oakbot.util;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
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
	 * Evaluates an expression, returning a {@link Element}.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @return the element or null if not found
	 */
	public Element element(String expression, Object node) {
		return (Element) node(expression, node);
	}

	/**
	 * Evaluates an expression, returning an iterable collection of {@link Node}
	 * s.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @return the node list
	 */
	public Iterable<Node> nodelist(String expression, Object node) {
		return it(_nodeList(expression, node));
	}

	/**
	 * Evaluates an expression, returning an iterable collection of
	 * {@link Element}s.
	 * @param expression the expression to evaluate
	 * @param node the parent node
	 * @return the node or null if not found
	 */
	public Iterable<Element> elements(String expression, Object node) {
		return it(_nodeList(expression, node), Element.class);
	}

	private NodeList _nodeList(String expression, Object node) {
		return (NodeList) eval(expression, node, XPathConstants.NODESET);
	}

	/**
	 * Returns the enclosed {@link XPath} object.
	 * @return the xpath object
	 */
	public XPath getXPath() {
		return xpath;
	}

	/**
	 * Returns an iterable collection containing the children of a {@link Node}.
	 * @param node the parent node
	 * @return the children
	 */
	public static Iterable<Node> children(Node node) {
		return it(node.getChildNodes());
	}

	/**
	 * Allows {@link NodeList} objects to be used in foreach loops.
	 * @param list the node list
	 * @return the iterable instance
	 */
	private static Iterable<Node> it(NodeList list) {
		return it(list, Node.class);
	}

	/**
	 * Allows {@link NodeList} objects to be used in foreach loops.
	 * @param list the node list
	 * @param clazz the class to cast each {@link Node} in the {@link NodeList}
	 * as.
	 * @return the iterable instance
	 */
	private static <T extends Node> Iterable<T> it(final NodeList list, final Class<T> clazz) {
		final Iterator<T> it = new Iterator<T>() {
			private int cur = 0;

			@Override
			public boolean hasNext() {
				return cur < list.getLength();
			}

			@Override
			public T next() {
				return clazz.cast(list.item(cur++));
			}
		};

		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return it;
			}
		};
	}
}
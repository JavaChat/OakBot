package oakbot.javadoc;

import java.util.List;

import org.jsoup.nodes.Document;

/**
 * Parses Javadoc HTML pages.
 * @author Michael Angstadt
 */
public interface PageParser {
	/**
	 * Parses the fully-qualified names of all the classes.
	 * @param document the HTML document
	 * @return the fully qualified names of each class names
	 */
	public List<String> parseClassNames(Document document);

	/**
	 * Parses a class page.
	 * @param document the HTML document
	 * @param className the fully-qualified class name
	 * @return the class info
	 */
	public ClassInfo parseClassPage(Document document, String className);

	/**
	 * Gets the base URL to use when parsing the document.
	 * @return the base URL
	 */
	public String getBaseUrl();
}

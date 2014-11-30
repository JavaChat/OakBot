package oakbot.javadoc;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Represents a Javadoc library.
 * @author Michael Angstadt
 */
public class JavadocLibrary {
	private final PageLoader loader;
	private final PageParser parser;

	/**
	 * @param loader the page loader
	 * @param parser the page parser
	 */
	public JavadocLibrary(PageLoader loader, PageParser parser) {
		this.loader = loader;
		this.parser = parser;
	}

	/**
	 * Gets the fully-qualified names of all the classes that are contained
	 * within this library.
	 * @return the fully-qualified class names
	 * @throws IOException if there was a problem reading from the Javadocs
	 */
	public List<String> getAllClassNames() throws IOException {
		Document document;
		try (InputStream in = loader.getAllClassesFile()) {
			document = Jsoup.parse(in, "UTF-8", parser.getBaseUrl());
		}
		return parser.parseClassNames(document);
	}

	/**
	 * Gets the Javadoc documentation of a class.
	 * @param className the fully-qualified class name (e.g. "java.lang.String")
	 * @return the documentation or null if the class was not found
	 * @throws IOException if there was a problem reading from the Javadocs
	 */
	public ClassInfo getClassInfo(String className) throws IOException {
		Document document;
		try (InputStream in = loader.getClassPage(className)) {
			if (in == null) {
				return null;
			}
			int pos = className.lastIndexOf('.');
			String path = className.substring(0, pos+1).replace('.', '/');
			String baseUrl = parser.getBaseUrl() + path;
			document = Jsoup.parse(in, "UTF-8", baseUrl);
		}
		return parser.parseClassPage(document, className);
	}
}
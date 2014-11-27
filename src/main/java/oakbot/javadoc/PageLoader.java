package oakbot.javadoc;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads Javadoc HTML pages
 * @author Michael Angstadt
 */
public interface PageLoader {
	/**
	 * Gets an input stream to the HTML page for a given class.
	 * @param className the fully qualified class name (e.g. "java.lang.String")
	 * @return an input stream to the HTML page or null if the class was not
	 * found
	 * @throws IOException if there's a problem reading the page
	 */
	InputStream getClassPage(String className) throws IOException;

	/**
	 * Gets the HTML file that list all the classes.
	 * @return an input stream to the HTML file
	 * @throws IOException if there's a problem reading the file
	 */
	InputStream getAllClassesFile() throws IOException;
}

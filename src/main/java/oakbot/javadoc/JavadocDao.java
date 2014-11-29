package oakbot.javadoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Retrieves class information from Javadoc files.
 * @author Michael Angstadt
 */
public class JavadocDao {
	private final Multimap<String, String> aliases = HashMultimap.create();
	private final Map<String, ClassInfo> cache = Collections.synchronizedMap(new HashMap<>());
	private final List<JavadocLibrary> libraries = new ArrayList<>();

	/**
	 * Adds a library's Javadoc API to this DAO.
	 * @param loader the page loader
	 * @param parser the page parser
	 * @throws IOException if there's a problem reading from the Javadocs
	 */
	public void addJavadocApi(PageLoader loader, PageParser parser) throws IOException {
		addJavadocApi(new JavadocLibrary(loader, parser));
	}

	/**
	 * Adds a library's Javadoc API to this DAO.
	 * @param library the Javadoc library
	 * @throws IOException if there's a problem reading from the parser
	 */
	public void addJavadocApi(JavadocLibrary library) throws IOException {
		//add all the class names to the simple name index
		for (String fullName : library.getAllClassNames()) {
			int dotPos = fullName.lastIndexOf('.');
			String simpleName = fullName.substring(dotPos + 1);

			aliases.put(simpleName.toLowerCase(), fullName);
			aliases.put(fullName.toLowerCase(), fullName);
			aliases.put(fullName, fullName);
		}

		libraries.add(library);
	}

	/**
	 * Gets the documentation on a class.
	 * @param className a fully-qualified class name (e.g. "java.lang.String")
	 * or a simple class name (e.g. "String").
	 * @return the class documentation or null if the class was not found
	 * @throws IOException if there's a problem reading the class's Javadocs
	 * @throws MultipleClassesFoundException if a simple name was passed into
	 * this method and multiple classes were found that have that name
	 */
	public ClassInfo getClassInfo(String className) throws IOException, MultipleClassesFoundException {
		Collection<String> names = aliases.get(className);
		if (names.isEmpty()) {
			names = aliases.get(className.toLowerCase());
		}

		if (names.isEmpty()) {
			return null;
		}

		if (names.size() > 1) {
			throw new MultipleClassesFoundException(names);
		}

		className = names.iterator().next();

		//check the cache
		ClassInfo info = cache.get(className);
		if (info != null) {
			return info;
		}

		//parse the class info from the Javadocs
		for (JavadocLibrary library : libraries) {
			info = library.getClassInfo(className);
			if (info != null) {
				cache.put(className, info);
				return info;
			}
		}

		return null;
	}
}

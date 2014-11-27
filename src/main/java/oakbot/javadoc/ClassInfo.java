package oakbot.javadoc;

import java.util.List;

/**
 * Holds the Javadoc info of a class.
 * @author Michael Angstadt
 */
public class ClassInfo {
	private final String fullName;
	private final String description;
	private final String url;
	private final List<String> modifiers;
	private final boolean deprecated;

	public ClassInfo(String fullName, String description, String url, List<String> modifiers, boolean deprecated) {
		this.fullName = fullName;
		this.description = description;
		this.url = url;
		this.modifiers = modifiers;
		this.deprecated = deprecated;
	}

	/**
	 * Gets the class's fully-qualified name.
	 * @return the fully-qualified name (e.g. "java.lang.String")
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Gets the class's description.
	 * @return the class description, formatted in SO Chat's markdown language
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the URL where this class's Javadocs can be viewed online.
	 * @return the URL or null if unknown
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Gets the modifiers of this class.
	 * @return the modifiers (e.g. "public", "final", "class")
	 */
	public List<String> getModifiers() {
		return modifiers;
	}

	/**
	 * Gets whether the class is deprecated or not.
	 * @return true if it's deprecated, false if not
	 */
	public boolean isDeprecated() {
		return deprecated;
	}
}

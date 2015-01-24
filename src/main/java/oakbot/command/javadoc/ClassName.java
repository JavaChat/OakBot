package oakbot.command.javadoc;

/**
 * Represents the name of a class.
 * @author Michael Angstadt
 */
public class ClassName {
	private final String fullyQualified, simple;

	/**
	 * @param fullyQualified the fully-qualified class name (e.g. "java.lang.String")
	 */
	public ClassName(String fullyQualified) {
		this.fullyQualified = fullyQualified;

		int pos = fullyQualified.lastIndexOf('.');
		simple = (pos < 0) ? fullyQualified : fullyQualified.substring(pos + 1);
	}

	/**
	 * @param fullyQualified the fully-qualified class name (e.g. "java.lang.String")
	 * @param simple the simple class name (e.g. "String")
	 */
	public ClassName(String fullyQualified, String simple) {
		this.fullyQualified = fullyQualified;
		this.simple = simple;
	}

	/**
	 * Gets the fully-qualified class name
	 * @return the fully-qualified class name (e.g. "java.lang.String")
	 */
	public String getFullyQualified() {
		return fullyQualified;
	}

	/**
	 * Gets the simple class name.
	 * @return the simple class name (e.g. "String")
	 */
	public String getSimple() {
		return simple;
	}

	@Override
	public String toString() {
		return fullyQualified;
	}
}

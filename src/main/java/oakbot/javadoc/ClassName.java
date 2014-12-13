package oakbot.javadoc;

/**
 * Represents the name of a class.
 * @author Michael Angstadt
 */
public class ClassName {
	private final String full, simple;

	/**
	 * @param full the fully-qualified class name (e.g. "java.lang.String")
	 */
	public ClassName(String full) {
		this.full = full;

		int pos = full.lastIndexOf('.');
		simple = (pos < 0) ? full : full.substring(pos + 1);
	}

	/**
	 * @param full the fully-qualified class name (e.g. "java.lang.String")
	 * @param simple the simple class name (e.g. "String")
	 */
	public ClassName(String full, String simple) {
		this.full = full;
		this.simple = simple;
	}

	/**
	 * Gets the fully-qualified class name
	 * @return the fully-qualified class name (e.g. "java.lang.String")
	 */
	public String getFull() {
		return full;
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
		return full;
	}
}

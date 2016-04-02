package oakbot.command.javadoc;

import java.util.Collections;
import java.util.List;

/**
 * Represents the name of a class.
 * @author Michael Angstadt
 */
public class ClassName {
	/*
	 * Note: The parts of the fully-qualified name must be separated out like
	 * this in order for the Javadoc URL of a class to be generated correctly.
	 */
	private final String packageName, simpleName, fullyQualifiedName;
	private final List<String> outerClassNames;

	/**
	 * @param packageName the package name (e.g. "java.util") or null for the
	 * default package
	 * @param simpleName the simple name (e.g. "Map")
	 */
	public ClassName(String packageName, String simpleName) {
		this(packageName, Collections.emptyList(), simpleName);
	}

	/**
	 * @param packageName the package name (e.g. "java.util") or null for the
	 * default package
	 * @param outerClassNames the outer classes that this class is inside of,
	 * outer-most to inner-most (e.g. "Map" for the "Map.Entry" class)
	 * @param simpleName the simple name (e.g. "Entry")
	 */
	public ClassName(String packageName, List<String> outerClassNames, String simpleName) {
		this.packageName = packageName;
		this.outerClassNames = Collections.unmodifiableList(outerClassNames);
		this.simpleName = simpleName;

		StringBuilder sb = new StringBuilder();
		if (packageName != null) {
			sb.append(packageName).append('.');
		}
		for (String outerClassName : outerClassNames) {
			sb.append(outerClassName).append('.');
		}
		sb.append(simpleName);
		fullyQualifiedName = sb.toString();
	}

	/**
	 * Gets the fully-qualified class name
	 * @return the fully-qualified class name (e.g. "java.lang.String")
	 */
	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}

	/**
	 * Gets the name of the package that this class belongs to.
	 * @return the package name (e.g. "java.lang") or null if it doesn't belong
	 * to a package
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Gets the simple names of the outer classes that contain this class.
	 * @return the outer class names, outer-most to inner-most (e.g. "Map" for
	 * the "Map.Entry" class)
	 */
	public List<String> getOuterClassNames() {
		return outerClassNames;
	}

	/**
	 * Gets the simple class name.
	 * @return the simple class name (e.g. "String")
	 */
	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String toString() {
		return getFullyQualifiedName();
	}
}

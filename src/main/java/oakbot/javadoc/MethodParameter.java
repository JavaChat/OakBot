package oakbot.javadoc;

/**
 * Contains information on a method parameter.
 * @author Michael Angstadt
 */
public class MethodParameter {
	private final String type, name;

	public MethodParameter(String type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * Gets the parameter type.
	 * @return the type (e.g. "int", "String", "ArrayList", etc). In the case of
	 * classes, this will always be the class's simple name
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the parameter name.
	 * @return the name
	 */
	public String getName() {
		return name;
	}
}

package oakbot.command.javadoc;

/**
 * Contains information on a method parameter.
 * @author Michael Angstadt
 */
public class ParameterInfo {
	private final ClassName type;
	private final String name;
	private final boolean array;
	private final String generic;

	public ParameterInfo(ClassName type, String name, boolean array, String generic) {
		this.type = type;
		this.name = name;
		this.array = array;
		this.generic = generic;
	}

	public ClassName getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public boolean isArray() {
		return array;
	}

	public String getGeneric() {
		return generic;
	}
}

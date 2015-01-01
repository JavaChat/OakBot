package oakbot.command.javadoc;

/**
 * Contains information on a method parameter.
 * @author Michael Angstadt
 */
public class ParameterInfo {
	private final ClassName type;
	private final String name, generic;
	private final boolean array, varargs;

	public ParameterInfo(ClassName type, String name, boolean array, boolean varargs, String generic) {
		this.type = type;
		this.name = name;
		this.array = array;
		this.varargs = varargs;
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

	public boolean isVarargs() {
		return varargs;
	}

	public String getGeneric() {
		return generic;
	}
}

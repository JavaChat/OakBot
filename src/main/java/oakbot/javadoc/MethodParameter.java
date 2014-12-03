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

	@Override
	public String toString() {
		return "MethodParameter [type=" + type + ", name=" + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		MethodParameter other = (MethodParameter) obj;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		if (type == null) {
			if (other.type != null) return false;
		} else if (!type.equals(other.type)) return false;
		return true;
	}
}

package oakbot.javadoc;

import java.util.List;

/**
 * Contains information on a method.
 * @author Michael Angstadt
 */
public class MethodInfo {
	private final String name;
	private final List<String> modifiers;
	private final List<MethodParameter> parameters;
	private final String description;
	private final String signatureString;
	private final boolean deprecated;

	public MethodInfo(String name, List<String> modifiers, List<MethodParameter> parameters, String description, String signatureString, boolean deprecated) {
		this.name = name;
		this.modifiers = modifiers;
		this.parameters = parameters;
		this.description = description;
		this.signatureString = signatureString;
		this.deprecated = deprecated;
	}

	public String getName() {
		return name;
	}

	public List<MethodParameter> getParameters() {
		return parameters;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getModifiers() {
		return modifiers;
	}

	public String getSignatureString() {
		return signatureString;
	}

	public boolean isDeprecated() {
		return deprecated;
	}
}

package oakbot.javadoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains information on a method.
 * @author Michael Angstadt
 */
public class MethodInfo {
	private final String name;
	private final List<String> modifiers;
	private final List<ParameterInfo> parameters;
	private final String description;
	private final String url;
	private final ClassName returnValue;
	private final boolean deprecated;

	private MethodInfo(Builder builder) {
		name = builder.name;
		modifiers = Collections.unmodifiableList(builder.modifiers);
		parameters = Collections.unmodifiableList(builder.parameters);
		description = builder.description;
		url = builder.url;
		returnValue = builder.returnValue;
		deprecated = builder.deprecated;
	}

	public String getName() {
		return name;
	}

	public List<ParameterInfo> getParameters() {
		return parameters;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getModifiers() {
		return modifiers;
	}

	public String getUrl() {
		return url;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public String getSignatureString() {
		StringBuilder sb = new StringBuilder();
		if (returnValue != null) {
			sb.append(returnValue.getSimple()).append(' ');
		}
		sb.append(name);

		sb.append('(');
		boolean first = true;
		for (ParameterInfo parameter : parameters) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}

			sb.append(parameter.getType().getSimple());
			String generic = parameter.getGeneric();
			if (generic != null) {
				sb.append(generic);
			}
			if (parameter.isArray()) {
				sb.append("[]");
			}

			sb.append(' ').append(parameter.getName());
		}
		sb.append(')');

		return sb.toString();
	}

	public static class Builder {
		private String name;
		private List<String> modifiers = new ArrayList<>();
		private List<ParameterInfo> parameters = new ArrayList<>();
		private String description;
		private String url;
		private ClassName returnValue;
		private boolean deprecated;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder modifiers(List<String> modifiers) {
			this.modifiers = modifiers;
			return this;
		}

		public Builder parameter(ParameterInfo parameter) {
			this.parameters.add(parameter);
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder url(String url) {
			this.url = url;
			return this;
		}

		public Builder returnValue(ClassName returnValue) {
			this.returnValue = returnValue;
			return this;
		}

		public Builder deprecated(boolean deprecated) {
			this.deprecated = deprecated;
			return this;
		}

		public MethodInfo build() {
			return new MethodInfo(this);
		}

	}
}

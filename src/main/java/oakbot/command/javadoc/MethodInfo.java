package oakbot.command.javadoc;

import java.util.List;

import com.google.common.collect.ImmutableList;

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
		modifiers = builder.modifiers.build();
		parameters = builder.parameters.build();
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

	/**
	 * Gets a string that uniquely identifies the method signature, as the
	 * compiler would.
	 * @return the signature string
	 */
	public String getSignature() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);

		sb.append('(');
		boolean first = true;
		for (ParameterInfo parameter : parameters) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}

			sb.append(parameter.getType().getFull());
			if (parameter.isArray()) {
				sb.append("[]");
			}
		}
		sb.append(')');

		return sb.toString();
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
		private ImmutableList.Builder<String> modifiers = ImmutableList.builder();
		private ImmutableList.Builder<ParameterInfo> parameters = ImmutableList.builder();
		private String description;
		private String url;
		private ClassName returnValue;
		private boolean deprecated;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder modifiers(List<String> modifiers) {
			this.modifiers.addAll(modifiers);
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

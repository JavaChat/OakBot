package oakbot.command.javadoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * <p>
 * Contains Javadoc information on a method.
 * </p>
 * <p>
 * This class is immutable. Use its {@link Builder} class to create new
 * instances.
 * </p>
 * @author Michael Angstadt
 */
public class MethodInfo {
	private final String name, description, urlAnchor, since;
	private final Set<String> modifiers;
	private final List<ParameterInfo> parameters;
	private final ClassName returnValue;
	private final boolean deprecated;

	private MethodInfo(Builder builder) {
		name = builder.name;
		modifiers = builder.modifiers.build();
		parameters = builder.parameters.build();
		since = builder.since;
		description = builder.description;
		returnValue = builder.returnValue;
		deprecated = builder.deprecated;

		StringBuilder sb = new StringBuilder();
		sb.append(name).append('-');
		List<String> fullNames = new ArrayList<>();
		for (ParameterInfo parameter : parameters) {
			String fullName = parameter.getType().getFullyQualifiedName();
			if (parameter.isArray()) {
				fullName += ":A";
			}
			if (parameter.isVarargs()) {
				fullName += "...";
			}
			fullNames.add(fullName);
		}
		sb.append(String.join("-", fullNames));
		sb.append('-');
		urlAnchor = sb.toString();
	}

	/**
	 * Gets the method name
	 * @return the method name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the method's parameters
	 * @return the method's parameters
	 */
	public List<ParameterInfo> getParameters() {
		return parameters;
	}

	/**
	 * Gets the value of the method's {@literal @since} tag.
	 * @return the {@literal @since} tag or null if it doesn't have one
	 */
	public String getSince() {
		return since;
	}

	/**
	 * Gets the method's Javadoc description.
	 * @return the description in SO-Chat Markdown syntax
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the method modifiers.
	 * @return the method modifiers (e.g. "public", "static")
	 */
	public Set<String> getModifiers() {
		return modifiers;
	}

	/**
	 * Gets this method's Javadoc URL anchor.
	 * @return the URL anchor (e.g. "substring-int-int-")
	 */
	public String getUrlAnchor() {
		return urlAnchor;
	}

	/**
	 * Gets whether this method is deprecated or not.
	 * @return true if it's deprecated, false if not
	 */
	public boolean isDeprecated() {
		return deprecated;
	}

	/**
	 * Gets a string that uniquely identifies the method signature, as the
	 * compiler would.
	 * @return the signature string (only includes the method name and the
	 * fully-qualified names of the parameters, e.g. "substring(int, int)")
	 */
	public String getSignature() {
		List<String> params = new ArrayList<>();
		for (ParameterInfo parameter : parameters) {
			params.add(parameter.getType().getFullyQualifiedName() + (parameter.isArray() ? "[]" : ""));
		}
		return name + "(" + String.join(", ", params) + ")";
	}

	/**
	 * Gets the signature string to display in the chat.
	 * @return the signature string for the chat
	 */
	public String getSignatureString() {
		StringBuilder sb = new StringBuilder();

		sb.append((returnValue == null) ? "void" : returnValue.getSimpleName()).append(' ');
		sb.append(name);

		List<String> params = new ArrayList<>();
		for (ParameterInfo parameter : parameters) {
			String type = parameter.getType().getSimpleName();
			String generic = parameter.getGeneric();
			if (generic != null) {
				type += generic;
			}
			if (parameter.isArray()) {
				type += "[]";
			}
			if (parameter.isVarargs()) {
				type += "...";
			}
			params.add(type + " " + parameter.getName());
		}
		sb.append('(').append(String.join(", ", params)).append(')');

		return sb.toString();
	}

	/**
	 * Builds new instances of {@link MethodInfo}.
	 */
	public static class Builder {
		private String name, description, since;
		private ImmutableSet.Builder<String> modifiers = ImmutableSet.builder();
		private ImmutableList.Builder<ParameterInfo> parameters = ImmutableList.builder();
		private ClassName returnValue;
		private boolean deprecated;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder modifiers(Collection<String> modifiers) {
			this.modifiers.addAll(modifiers);
			return this;
		}

		public Builder parameter(ParameterInfo parameter) {
			this.parameters.add(parameter);
			return this;
		}

		public Builder since(String since) {
			this.since = since;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
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

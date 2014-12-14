package oakbot.command.javadoc;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Holds the Javadoc info of a class.
 * @author Michael Angstadt
 */
public class ClassInfo {
	private final ClassName name, superClass;
	private final String description, url;
	private final List<String> modifiers;
	private final List<ClassName> interfaces;
	private final Multimap<String, MethodInfo> methods;
	private final boolean deprecated;

	private ClassInfo(Builder builder) {
		name = builder.name;
		superClass = builder.superClass;
		description = builder.description;
		url = builder.url;
		modifiers = builder.modifiers.build();
		interfaces = builder.interfaces.build();
		methods = builder.methods.build();
		deprecated = builder.deprecated;
	}

	public ClassName getName() {
		return name;
	}

	public ClassName getSuperClass() {
		return superClass;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	public List<String> getModifiers() {
		return modifiers;
	}

	public List<ClassName> getInterfaces() {
		return interfaces;
	}

	public Collection<MethodInfo> getMethod(String name) {
		return methods.get(name.toLowerCase());
	}

	public Collection<MethodInfo> getMethods() {
		return methods.values();
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public static class Builder {
		private ClassName name, superClass;
		private String description, url;
		private ImmutableList.Builder<String> modifiers = ImmutableList.builder();
		private ImmutableList.Builder<ClassName> interfaces = ImmutableList.builder();
		private ImmutableMultimap.Builder<String, MethodInfo> methods = ImmutableMultimap.builder();
		private boolean deprecated = false;

		public Builder name(String full, String simple) {
			this.name = new ClassName(full, simple);
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

		public Builder superClass(String superClass) {
			this.superClass = new ClassName(superClass);
			return this;
		}

		public Builder modifiers(List<String> modifiers) {
			this.modifiers.addAll(modifiers);
			return this;
		}

		public Builder interface_(String interface_) {
			interfaces.add(new ClassName(interface_));
			return this;
		}

		public Builder interfaces(List<ClassName> interfaces) {
			this.interfaces.addAll(interfaces);
			return this;
		}

		public Builder method(MethodInfo method) {
			this.methods.put(method.getName().toLowerCase(), method);
			return this;
		}

		public Builder deprecated(boolean deprecated) {
			this.deprecated = deprecated;
			return this;
		}

		public ClassInfo build() {
			return new ClassInfo(this);
		}
	}
}

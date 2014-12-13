package oakbot.javadoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the Javadoc info of a class.
 * @author Michael Angstadt
 */
public class ClassInfo {
	private final ClassName name, superClass;
	private final String description, url;
	private final List<String> modifiers;
	private final List<ClassName> interfaces;
	private final List<MethodInfo> methods;
	private final boolean deprecated;

	private ClassInfo(Builder builder) {
		name = builder.name;
		superClass = builder.superClass;
		description = builder.description;
		url = builder.url;
		modifiers = Collections.unmodifiableList(builder.modifiers);
		interfaces = Collections.unmodifiableList(builder.interfaces);
		methods = Collections.unmodifiableList(builder.methods);
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

	public List<MethodInfo> getMethods() {
		return methods;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public static class Builder {
		private ClassName name, superClass;
		private String description, url;
		private List<String> modifiers = new ArrayList<>();
		private List<ClassName> interfaces = new ArrayList<>();
		private List<MethodInfo> methods = new ArrayList<>();;
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
			this.modifiers = modifiers;
			return this;
		}
		
		public Builder interface_(String interface_){
			interfaces.add(new ClassName(interface_));
			return this;
		}

		public Builder interfaces(List<ClassName> interfaces) {
			this.interfaces = interfaces;
			return this;
		}

		public Builder methods(List<MethodInfo> methods) {
			this.methods = methods;
			return this;
		}

		public Builder method(MethodInfo method) {
			this.methods.add(method);
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

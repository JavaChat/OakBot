package oakbot.command.javadoc;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Holds the Javadoc info of a class.
 * @author Michael Angstadt
 */
public class ClassInfo {
	private final ClassName name, superClass;
	private final String description;
	private final Set<String> modifiers;
	private final Set<ClassName> interfaces;
	private final Multimap<String, MethodInfo> methods;
	private final boolean deprecated;
	private final LibraryZipFile zipFile;

	private ClassInfo(Builder builder) {
		name = builder.name;
		superClass = builder.superClass;
		description = builder.description;
		modifiers = builder.modifiers.build();
		interfaces = builder.interfaces.build();
		methods = builder.methods.build();
		deprecated = builder.deprecated;
		zipFile = builder.zipFile;
	}

	/**
	 * Gets the name of the class.
	 * @return the class name
	 */
	public ClassName getName() {
		return name;
	}

	/**
	 * Gets the name of the class's parent class.
	 * @return the parent class name or null if it doesn't have one
	 */
	public ClassName getSuperClass() {
		return superClass;
	}

	/**
	 * Gets the class description.
	 * @return the class description (in SO-Chat markdown)
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the URL to this class's Javadoc page (with frames).
	 * @return the URL or null if no base URL was given
	 */
	public String getFrameUrl() {
		return zipFile.getFrameUrl(this);
	}

	/**
	 * Gets the URL to this class's Javadoc page (without frames).
	 * @return the URL or null if no base URL was given
	 */
	public String getUrl() {
		return zipFile.getUrl(this);
	}

	/**
	 * Gets the class modifiers.
	 * @return the class modifiers (e.g. "public, final")
	 */
	public Set<String> getModifiers() {
		return modifiers;
	}

	/**
	 * Gets the names of the interfaces that the class implements.
	 * @return the class's interfaces
	 */
	public Set<ClassName> getInterfaces() {
		return interfaces;
	}

	/**
	 * Gets info on all of the methods that have the given name.
	 * @param name the method name (case insensitive)
	 * @return the methods
	 */
	public Collection<MethodInfo> getMethod(String name) {
		return methods.get(name.toLowerCase());
	}

	/**
	 * Gets the class's methods.
	 * @return the class's methods
	 */
	public Collection<MethodInfo> getMethods() {
		return methods.values();
	}

	/**
	 * Determines whether the class is deprecated or not.
	 * @return true if it's deprecated, false if not
	 */
	public boolean isDeprecated() {
		return deprecated;
	}

	/**
	 * Gets the ZIP file that the class's parsed Javadoc info is stored in.
	 * @return the ZIP file
	 */
	public LibraryZipFile getZipFile() {
		return zipFile;
	}

	/**
	 * Builds new instances of {@link ClassInfo}.
	 */
	public static class Builder {
		private ClassName name;
		private ClassName superClass;
		private String description;
		private ImmutableSet.Builder<String> modifiers = ImmutableSet.builder();
		private ImmutableSet.Builder<ClassName> interfaces = ImmutableSet.builder();
		private ImmutableMultimap.Builder<String, MethodInfo> methods = ImmutableMultimap.builder();
		private boolean deprecated = false;
		private LibraryZipFile zipFile;

		public Builder name(String full, String simple) {
			this.name = new ClassName(full, simple);
			return this;
		}

		public Builder description(String description) {
			this.description = description;
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

		public Builder zipFile(LibraryZipFile zipFile) {
			this.zipFile = zipFile;
			return this;
		}

		public ClassInfo build() {
			return new ClassInfo(this);
		}
	}
}

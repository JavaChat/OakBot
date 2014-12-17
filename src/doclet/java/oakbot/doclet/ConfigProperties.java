package oakbot.doclet;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import oakbot.util.PropertiesWrapper;

/**
 * Gets the doclet's configuration properties.
 * @author Michael Angstadt
 */
public class ConfigProperties {
	private final PropertiesWrapper properties;

	public ConfigProperties(Properties properties) {
		this.properties = new PropertiesWrapper(properties);
	}

	public Path getOutputPath() {
		String value = properties.get("oakbot.doclet.output.path");
		return (value == null) ? null : Paths.get(value);
	}

	public boolean isPrettyPrint() {
		String value = properties.get("oakbot.doclet.output.path");
		return (value == null) ? false : Boolean.parseBoolean(value);
	}

	public String getLibraryName() {
		return properties.get("oakbot.doclet.library.name");
	}

	public String getLibraryBaseUrl() {
		return properties.get("oakbot.doclet.library.baseUrl");
	}
}

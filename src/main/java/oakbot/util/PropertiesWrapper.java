package oakbot.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Contains utility methods for use with {@link Properties} classes.
 * @author Michael Angstadt
 */
public class PropertiesWrapper {
	private final Properties properties;

	public PropertiesWrapper() {
		this(new Properties());
	}

	public PropertiesWrapper(Properties properties) {
		this.properties = properties;
	}

	public String get(String key) {
		return get(key, null);
	}

	public String get(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public void set(String key, Object value) {
		if (value == null) {
			remove(key);
		} else {
			properties.setProperty(key, value.toString());
		}
	}

	public void remove(String key) {
		properties.remove(key);
	}

	public Integer getInt(String key) {
		return getInt(key, null);
	}

	public Integer getInt(String key, Integer defaultValue) {
		String value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return Integer.valueOf(value);
	}

	public List<Integer> getIntList(String key) {
		return getIntList(key, Collections.emptyList());
	}

	public List<Integer> getIntList(String key, List<Integer> defaultValue) {
		String value = get(key);
		if (value == null) {
			return defaultValue;
		}

		List<Integer> rooms = new ArrayList<>();
		for (String v : value.split("\\s*,\\s*")) { //split by comma
			Integer room = Integer.valueOf(v);
			rooms.add(room);
		}
		return rooms;
	}
}

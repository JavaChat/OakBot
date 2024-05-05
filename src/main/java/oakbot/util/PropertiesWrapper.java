package oakbot.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wraps a {@link Properties} object, providing additional functionality.
 * @author Michael Angstadt
 */
public class PropertiesWrapper implements Iterable<Map.Entry<String, String>> {
	private final Properties properties;
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Creates an empty properties list.
	 */
	public PropertiesWrapper() {
		this(new Properties());
	}

	/**
	 * Creates a properties list based on an existing properties object.
	 * @param properties the properties object
	 */
	public PropertiesWrapper(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Loads a properties list from a file.
	 * @param file the path to the file
	 * @throws IOException if there was a problem loading the file
	 */
	public PropertiesWrapper(Path file) throws IOException {
		this();
		try (var reader = Files.newBufferedReader(file, Charset.defaultCharset())) {
			properties.load(reader);
		}
	}

	/**
	 * Gets a property value. This method trims the property value and returns
	 * null if the trimmed value is empty.
	 * @param key the key
	 * @return the value or null if not found or empty
	 */
	public String get(String key) {
		return get(key, null);
	}

	/**
	 * Gets a property value. This method trims the property value and returns
	 * the default value if the trimmed value is empty.
	 * @param key the key
	 * @param defaultValue the value to return if the property does not exist or
	 * the property value is empty
	 * @return the value
	 */
	public String get(String key, String defaultValue) {
		var value = properties.getProperty(key);
		if (value != null) {
			value = value.trim();
		}

		return (value == null || value.isEmpty()) ? defaultValue : value;
	}

	/**
	 * <p>
	 * Sets a property value.
	 * </p>
	 * <p>
	 * The value's {@code toString()} is called in order to generate a string
	 * value. If the object is a {@link LocalDateTime}, then its string value
	 * will be generated using an internal {@link DateTimeFormatter} object.
	 * <p>
	 * @param key the key
	 * @param value the value or null to remove
	 */
	public void set(String key, Object value) {
		if (value == null) {
			remove(key);
			return;
		}

		var valueStr = (value instanceof LocalDateTime dateTime) ? dateTime.format(dateTimeFormatter) : value.toString();
		properties.setProperty(key, valueStr);
	}

	/**
	 * Removes a property.
	 * @param key the key
	 * @return the value that was removed
	 */
	public String remove(String key) {
		return (String) properties.remove(key);
	}

	/**
	 * Gets a file path property value.
	 * @param key the key
	 * @return the value or null if not found
	 */
	public Path getPath(String key) {
		return getPath(key, null);
	}

	/**
	 * Gets a file path property value.
	 * @param key the key
	 * @param defaultValue the value to return if the property does not exist
	 * @return the value
	 */
	public Path getPath(String key, Path defaultValue) {
		var value = get(key);
		return (value == null) ? defaultValue : Paths.get(value);
	}

	/**
	 * Gets an integer property value.
	 * @param key the key
	 * @return the value or null if not found
	 * @throws NumberFormatException if it cannot parse the value as an integer
	 */
	public Integer getInteger(String key) {
		return getInteger(key, null);
	}

	/**
	 * Gets an integer property value.
	 * @param key the key
	 * @param defaultValue the value to return if the property does not exist
	 * @return the value
	 * @throws NumberFormatException if it could parse the value as an integer
	 */
	public Integer getInteger(String key, Integer defaultValue) {
		var value = get(key);
		return (value == null) ? defaultValue : Integer.valueOf(value);
	}

	/**
	 * Parses a comma-delimited list of integers. If any non-integer values are
	 * encountered, they will be skipped.
	 * @param key the key
	 * @return the list or empty list if not found
	 */
	public List<Integer> getIntegerList(String key) {
		return getIntegerList(key, new ArrayList<>(0));
	}

	/**
	 * Parses a comma-delimited list of integers. If any non-integer values are
	 * encountered, they will be skipped.
	 * @param key the key
	 * @param defaultValue the list to return if the property does not exist
	 * @return the list
	 */
	public List<Integer> getIntegerList(String key, List<Integer> defaultValue) {
		var value = get(key);
		if (value == null) {
			return defaultValue;
		}

		//@formatter:off
		return Arrays.stream(value.split("\\s*,\\s*"))
			.filter(v -> v.matches("\\d+"))
			.map(Integer::valueOf)
		.toList();
		//@formatter:on
	}

	/**
	 * Sets a comma-delimited list of integers.
	 * @param key the key
	 * @param list the list or null to remove
	 */
	public void setIntegerList(String key, List<Integer> list) {
		String value;

		if (list == null) {
			value = null;
		} else {
			//@formatter:off
			value = list.stream()
				.map(Object::toString)
			.collect(Collectors.joining(","));
			//@formatter:on
		}

		set(key, value);
	}

	/**
	 * Gets a date property value.
	 * @param key the key
	 * @return the value or null if not found
	 * @throws DateTimeParseException if it could not parse the value as a date
	 */
	public LocalDateTime getDate(String key) throws DateTimeParseException {
		var value = get(key);
		return (value == null) ? null : LocalDateTime.parse(value, dateTimeFormatter);
	}

	/**
	 * Gets a boolean property value.
	 * @param key the key
	 * @return the value or null if not found
	 */
	public Boolean getBoolean(String key) {
		return getBoolean(key, null);
	}

	/**
	 * Gets a boolean property value.
	 * @param key the key
	 * @param defaultValue the value to return if the property does not exist
	 * @return the value
	 */
	public Boolean getBoolean(String key, Boolean defaultValue) {
		var value = get(key);
		return (value == null) ? defaultValue : Boolean.valueOf(value);
	}

	/**
	 * Gets a file path property value.
	 * @param key the key
	 * @return the value or null if not found
	 */
	public Path getFile(String key) {
		var value = get(key);
		return (value == null) ? null : Paths.get(value);
	}

	/**
	 * Writes the properties to disk.
	 * @param file the path to write the properties to
	 * @throws IOException
	 */
	public void store(Path file) throws IOException {
		store(file, "");
	}

	/**
	 * Writes the properties to disk.
	 * @param file the path to write the properties to
	 * @param comment the comment to include at the top of the file.
	 * @throws IOException
	 */
	public void store(Path file, String comment) throws IOException {
		try (var writer = Files.newBufferedWriter(file, Charset.defaultCharset())) {
			properties.store(writer, comment);
		}
	}

	/**
	 * Gets all the property keys.
	 * @return the property keys
	 */
	public Set<String> keySet() {
		var keySet = properties.keySet();

		//@formatter:off
		return keySet.stream()
			.map(k -> (String)k)
		.collect(Collectors.toSet());
		//@formatter:;on
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		return new Iterator<Entry<String, String>>() {
			private final Iterator<Entry<Object, Object>> it = properties.entrySet().iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Entry<String, String> next() {
				var entry = it.next();
				return new EntryImpl(entry);
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}

	private static class EntryImpl implements Entry<String, String> {
		private final Entry<Object, Object> entry;

		public EntryImpl(Entry<Object, Object> entry) {
			this.entry = entry;
		}

		@Override
		public String getKey() {
			return (String) entry.getKey();
		}

		@Override
		public String getValue() {
			return (String) entry.getValue();
		}

		@Override
		public String setValue(String value) {
			return (String) entry.setValue(value);
		}
	}
}

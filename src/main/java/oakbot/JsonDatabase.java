package oakbot;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;

import oakbot.util.JsonUtils;

/**
 * A database that persists its values to a JSON file.
 * @author Michael Angstadt
 */
public class JsonDatabase implements Database {
	private static final Logger logger = Logger.getLogger(JsonDatabase.class.getName());

	private final Path file;
	private final Map<String, Object> fields = new HashMap<>();
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private boolean changed = false;

	/**
	 * @param path the path to the JSON file the data is stored in
	 * @throws IOException if there's a problem reading the file
	 */
	public JsonDatabase(String path) throws IOException {
		this(Paths.get(path));
	}

	/**
	 * @param file the JSON file the data is stored in
	 * @throws IOException if there's a problem reading the file
	 */
	public JsonDatabase(Path file) throws IOException {
		this.file = file;
		if (Files.exists(file)) {
			load();
		}
	}

	/**
	 * Loads existing data from the file.
	 * @throws IOException if there's a problem reading the file
	 */
	private void load() throws IOException {
		JsonNode root;
		try (Reader reader = Files.newBufferedReader(file)) {
			root = JsonUtils.parse(reader);
		}

		root.fields().forEachRemaining(field -> { //@formatter:off
			var name = field.getKey();
			var value = parseNode(field.getValue());
			fields.put(name, value);
		}); //@formatter:on
	}

	private Object parseNode(JsonNode node) {
		if (node.isArray()) {
			//@formatter:off
			return JsonUtils.streamArray(node)
				.map(this::parseNode)
			.toList();
			//@formatter:on
		}

		if (node.isObject()) {
			/*
			 * Note: Collectors.toMap() cannot be used here because it throws a
			 * NPE if any values in the map are null.
			 */
			Map<String, Object> map = new HashMap<>();
			node.fields().forEachRemaining(field -> {
				var name = field.getKey();
				var value = parseNode(field.getValue());
				map.put(name, value);
			});
			return map;
		}

		if (node.isInt()) {
			return node.asInt();
		}

		if (node.isLong()) {
			return node.asLong();
		}

		if (node.isNull()) {
			return null;
		}

		var text = node.asText();

		try {
			return LocalDateTime.parse(text, dateTimeFormatter);
		} catch (DateTimeParseException e) {
			//not a date string
		}

		return text;
	}

	@Override
	public Object get(String key) {
		return fields.get(key);
	}

	@Override
	public void set(String key, Object value) {
		fields.put(key, value);
		changed = true;
	}

	@Override
	public void commit() {
		if (!changed) {
			return;
		}

		try (Writer writer = Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING)) {
			var factory = new JsonFactory();
			try (var generator = factory.createGenerator(writer)) {
				generator.setPrettyPrinter(new DefaultPrettyPrinter());
				generator.writeStartObject();

				for (var entry : fields.entrySet()) {
					var fieldName = entry.getKey();
					var value = entry.getValue();

					generator.writeFieldName(fieldName);
					write(generator, value);
				}

				generator.writeEndObject();
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Could not persist database.");
		}

		changed = false;
	}

	private void write(JsonGenerator generator, Object value) throws IOException {
		if (value instanceof Map<?, ?> map) {
            generator.writeStartObject();

			for (var entry : map.entrySet()) {
				var fieldName = entry.getKey().toString();
				var fieldValue = entry.getValue();

				generator.writeFieldName(fieldName);
				write(generator, fieldValue);
			}

			generator.writeEndObject();
			return;
		}

		if (value instanceof Collection<?> list) {
            generator.writeStartArray();

			for (var item : list) {
				write(generator, item);
			}

			generator.writeEndArray();
			return;
		}

		if (value instanceof LocalDateTime date) {
            generator.writeString(date.format(dateTimeFormatter));
			return;
		}

		if (value instanceof Integer integer) {
            generator.writeNumber(integer);
			return;
		}

		if (value instanceof Long integer) {
            generator.writeNumber(integer);
			return;
		}

		if (value == null) {
			generator.writeNull();
			return;
		}

		var string = value.toString();
		generator.writeString(string);
	}
}

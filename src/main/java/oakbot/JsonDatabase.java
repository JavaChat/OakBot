package oakbot;

import static com.google.common.collect.Streams.stream;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		ObjectMapper mapper = new ObjectMapper();
		try (Reader reader = Files.newBufferedReader(file)) {
			root = mapper.readTree(reader);
		}

		root.fields().forEachRemaining(field -> { //@formatter:off
			String name = field.getKey();
			Object value = parseNode(field.getValue());
			fields.put(name, value);
		}); //@formatter:on
	}

	private Object parseNode(JsonNode node) {
		if (node.isArray()) {
			return stream(node) //@formatter:off
				.map(this::parseNode)
			.collect(Collectors.toList()); //@formatter:on
		}

		if (node.isObject()) {
			/*
			 * Note: Collectors.toMap() cannot be used here because it throws a
			 * NPE if any values in the map are null.
			 */
			Map<String, Object> map = new HashMap<>();
			node.fields().forEachRemaining(field -> {
				String name = field.getKey();
				Object value = parseNode(field.getValue());
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

		String text = node.asText();

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
			JsonFactory factory = new JsonFactory();
			try (JsonGenerator generator = factory.createGenerator(writer)) {
				generator.setPrettyPrinter(new DefaultPrettyPrinter());
				generator.writeStartObject();

				for (Map.Entry<String, Object> entry : fields.entrySet()) {
					String fieldName = entry.getKey();
					Object value = entry.getValue();

					generator.writeFieldName(fieldName);
					write(generator, value);
				}

				generator.writeEndObject();
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not persist database.", e);
		}

		changed = false;
	}

	private void write(JsonGenerator generator, Object value) throws IOException {
		if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			generator.writeStartObject();

			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String fieldName = entry.getKey().toString();
				Object fieldValue = entry.getValue();

				generator.writeFieldName(fieldName);
				write(generator, fieldValue);
			}

			generator.writeEndObject();
			return;
		}

		if (value instanceof Collection) {
			Collection<?> list = (Collection<?>) value;
			generator.writeStartArray();

			for (Object item : list) {
				write(generator, item);
			}

			generator.writeEndArray();
			return;
		}

		if (value instanceof LocalDateTime) {
			LocalDateTime date = (LocalDateTime) value;
			generator.writeString(date.format(dateTimeFormatter));
			return;
		}

		if (value instanceof Integer) {
			Integer integer = (Integer) value;
			generator.writeNumber(integer);
			return;
		}

		if (value instanceof Long) {
			Long integer = (Long) value;
			generator.writeNumber(integer);
			return;
		}

		if (value == null) {
			generator.writeNull();
			return;
		}

		String string = value.toString();
		generator.writeString(string);
	}
}

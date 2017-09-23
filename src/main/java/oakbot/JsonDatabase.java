package oakbot;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

		Iterator<String> it = root.fieldNames();
		while (it.hasNext()) {
			String fieldName = it.next();
			JsonNode field = root.get(fieldName);
			Object value = parseNode(field);
			fields.put(fieldName, value);
		}
	}

	private Object parseNode(JsonNode node) {
		if (node.isArray()) {
			List<Object> list = new ArrayList<Object>();
			Iterator<JsonNode> it = node.elements();
			while (it.hasNext()) {
				JsonNode element = it.next();
				Object parsedElement = parseNode(element);
				list.add(parsedElement);
			}
			return list;
		}

		if (node.isObject()) {
			Map<String, Object> map = new HashMap<>();
			Iterator<String> it = node.fieldNames();
			while (it.hasNext()) {
				String fieldName = it.next();
				JsonNode field = node.get(fieldName);
				Object parsedElement = parseNode(field);
				map.put(fieldName, parsedElement);
			}
			return map;
		}

		if (node.isInt()) {
			return node.asInt();
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

		StandardOpenOption[] openOptions;
		if (Files.exists(file)) {
			openOptions = new StandardOpenOption[] { StandardOpenOption.TRUNCATE_EXISTING };
		} else {
			openOptions = new StandardOpenOption[] {};
		}

		try (Writer writer = Files.newBufferedWriter(file, openOptions)) {
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
				Object v = entry.getValue();

				generator.writeFieldName(fieldName);
				write(generator, v);
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

		if (value == null) {
			generator.writeNull();
			return;
		}

		String string = value.toString();
		generator.writeString(string);
	}
}

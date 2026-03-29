package oakbot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON utility methods.
 * @author Michael Angstadt
 */
public final class JsonUtils {
	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Creates a new object node that's not attached to anything.
	 * @return the object node
	 */
	public static ObjectNode newObject() {
		return mapper.createObjectNode();
	}

	/**
	 * Parses JSON from an input stream.
	 * @param in the input stream
	 * @return the parsed JSON
	 * @throws IOException if there's a problem reading from the input stream or
	 * parsing the JSON
	 */
	public static JsonNode parse(InputStream in) throws IOException {
		return mapper.readTree(in);
	}

	/**
	 * Parses JSON from a reader.
	 * @param reader the reader
	 * @return the parsed JSON
	 * @throws IOException if there's a problem reading from the reader or
	 * parsing the JSON
	 */
	public static JsonNode parse(Reader reader) throws IOException {
		return mapper.readTree(reader);
	}

	/**
	 * Pretty prints the given JSON node.
	 * @param node the JSON node
	 * @return the pretty-printed JSON
	 * @throws JsonProcessingException if the node couldn't be written to a
	 * string (unlikely)
	 */
	public static String prettyPrint(JsonNode node) throws JsonProcessingException {
		return toString(node, new DefaultPrettyPrinter());
	}

	/**
	 * Pretty prints the given JSON node.
	 * @param node the JSON node
	 * @return the pretty-printed JSON or an error message if the node couldn't
	 * be written to a string (unlikely)
	 */
	public static String prettyPrintForLogging(JsonNode node) {
		try {
			return prettyPrint(node);
		} catch (JsonProcessingException e) {
			return "Unable to write JSON node to string: " + e.getMessage();
		}
	}

	/**
	 * Converts the given JSON node to a string.
	 * @param node the JSON node
	 * @return the JSON string
	 * @throws JsonProcessingException if the node couldn't be written to a
	 * string (unlikely)
	 */
	public static String toString(JsonNode node) throws JsonProcessingException {
		return toString(node, null);
	}

	private static String toString(JsonNode node, PrettyPrinter pp) throws JsonProcessingException {
		return mapper.writer(pp).writeValueAsString(node);
	}

	/**
	 * Extracts a single value from a JSON node using a path expression.
	 * @param node the JSON node
	 * @param path the path to the value (strings and integers only, e.g.
	 * {@code ["data", 0, "url"]} translates to
	 * {@code node.path("data").path(0).path("url").asText()})
	 * @return the value
	 */
	public static Optional<String> extractField(JsonNode node, Object... path) {
		var n = node;

		for (var field : path) {
			if (field instanceof String fieldStr) {
				n = n.path(fieldStr);
			} else if (field instanceof Integer fieldInt) {
				n = n.path(fieldInt);
			} else {
				throw new IllegalArgumentException("Only strings and ints allowed in field path.");
			}

			if (n.isMissingNode()) {
				return Optional.empty();
			}
		}

		return Optional.of(n.asText());
	}

	/**
	 * Adds a field to a JSON object only if the field value is non-null.
	 * @param node the JSON object
	 * @param fieldName the field name
	 * @param value the field value
	 */
	public static void putIfNotNull(ObjectNode node, String fieldName, String value) {
		if (value != null) {
			node.put(fieldName, value);
		}
	}

	/**
	 * Adds a field to a JSON object only if the field value is non-null.
	 * @param node the JSON object
	 * @param fieldName the field name
	 * @param value the field value
	 */
	public static void putIfNotNull(ObjectNode node, String fieldName, Integer value) {
		if (value != null) {
			node.put(fieldName, value);
		}
	}

	/**
	 * Adds a field to a JSON object only if the field value is non-null.
	 * @param node the JSON object
	 * @param fieldName the field name
	 * @param value the field value
	 */
	public static void putIfNotNull(ObjectNode node, String fieldName, Double value) {
		if (value != null) {
			node.put(fieldName, value);
		}
	}

	/**
	 * Adds a field to a JSON object only if the field value is non-null.
	 * @param node the JSON object
	 * @param fieldName the field name
	 * @param value the field value
	 */
	public static void putIfNotNull(ObjectNode node, String fieldName, Boolean value) {
		if (value != null) {
			node.put(fieldName, value);
		}
	}

	/**
	 * Parses a JSON value representing a Unix timestamp.
	 * @param value the JSON value
	 * @return the timestamp
	 */
	public static Instant asEpochSecond(JsonNode value) {
		return Instant.ofEpochSecond(value.asLong());
	}

	private JsonUtils() {
		//hide constructor
	}
}

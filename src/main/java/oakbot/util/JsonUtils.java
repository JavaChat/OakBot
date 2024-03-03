package oakbot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
	 */
	public static String prettyPrint(JsonNode node) {
		return toString(node, new DefaultPrettyPrinter());
	}

	/**
	 * Converts the given JSON node to a string.
	 * @param node the JSON node
	 * @return the JSON string
	 */
	public static String toString(JsonNode node) {
		return toString(node, null);
	}

	private static String toString(JsonNode node, PrettyPrinter pp) {
		ObjectWriter writer = mapper.writer(pp);
		try {
			return writer.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extracts a single value from a JSON node using a path expression.
	 * @param path the path to the value (e.g. "data/0/url" translates to
	 * {@code node.get("data").get(0).get("url").asText()})
	 * @param node the JSON node
	 * @return the value
	 * @throws IllegalArgumentException if the path wasn't valid
	 */
	public static String extractField(String path, JsonNode node) {
		JsonNode n = node;
		String[] fields = path.split("/");

		for (String field : fields) {
			try {
				int index = Integer.parseInt(field);
				n = n.path(index);
			} catch (NumberFormatException e) {
				n = n.path(field);
			}

			if (n.isMissingNode()) {
				throw new IllegalArgumentException("JSON path not found: " + path);
			}
		}

		return n.asText();
	}

	private JsonUtils() {
		//hide constructor
	}
}

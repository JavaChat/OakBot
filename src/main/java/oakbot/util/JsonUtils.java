package oakbot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * JSON utility methods.
 * @author Michael Angstadt
 */
public final class JsonUtils {
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
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer(pp);
		try {
			return writer.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private JsonUtils() {
		//hide constructor
	}
}

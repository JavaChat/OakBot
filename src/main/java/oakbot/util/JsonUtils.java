package oakbot.util;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory factory = new JsonFactory();
		StringWriter writer = new StringWriter();
		try {
			JsonGenerator generator = factory.createGenerator(writer);
			generator.setPrettyPrinter(new DefaultPrettyPrinter());
			mapper.writeTree(generator, node);
			generator.close();
			return writer.toString();
		} catch (IOException e) {
			//should never be thrown
			throw new RuntimeException(e);
		}
	}

	private JsonUtils() {
		//hide constructor
	}
}

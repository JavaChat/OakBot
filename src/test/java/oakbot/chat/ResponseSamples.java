package oakbot.chat;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Helper class for creating examples of the kind of responses that are returned
 * from the Stack Overflow Chat API. For unit testing.
 * @author Michael Angstadt
 */
public final class ResponseSamples {
	/**
	 * Gets the HTML of the login page.
	 * @param fkey the fkey to populate the page with.
	 * @return the HTML page
	 * @throws IOException
	 */
	public static String loginPage(String fkey) throws IOException {
		String html = readFile("users-login.html");
		return html.replace("${fkey}", fkey);
	}

	/**
	 * Gets the HTML of a chat room that the bot has permission to post to.
	 * @param fkey the fkey to populate the page with
	 * @param canPost true if the user is allowed to post messages to the room,
	 * false if not
	 * @return the HTML page
	 * @throws IOException
	 */
	public static String chatRoom(String fkey) throws IOException {
		String html = readFile("rooms-1.html");
		return html.replace("${fkey}", fkey);
	}

	/**
	 * Gets the HTML of a chat room that the bot does not have permission to
	 * post to.
	 * @param fkey the fkey to populate the page with
	 * @return the chat room HTML
	 * @throws IOException
	 */
	public static String protectedChatRoom(String fkey) throws IOException {
		String html = readFile("rooms-15-protected.html");
		return html.replace("${fkey}", fkey);
	}

	/**
	 * Generates the response for requesting a web socket URI.
	 * @param url the URI of the web socket
	 * @return the JSON response
	 */
	public static String wsAuth(String url) {
		//@formatter:off
		return "{" +
			"\"url\": \"" + url + "\"" +
		"}";
		//@formatter:on
	}

	/**
	 * Generates the response for posting a new message.
	 * @param id the ID of the new message
	 * @return the JSON response
	 */
	public static String newMessage(long id) {
		//@formatter:off
		return "{" +
			"\"id\": " + id + "," +
			"\"time\": " + (System.currentTimeMillis() / 1000) +
		"}";
		//@formatter:on
	}

	/**
	 * Generates the response for requesting the recent events of a chat room.
	 * @return a builder object for building the response
	 */
	public static EventsResponseBuilder events() {
		return new EventsResponseBuilder();
	}

	public static class EventsResponseBuilder {
		private final StringBuilder sb = new StringBuilder();
		private boolean first = true;

		public EventsResponseBuilder() {
			sb.append("{\"events\":[");
		}

		/**
		 * Adds an event to the response.
		 * @param timestamp the timestamp
		 * @param content the message content
		 * @param userId the ID of the user who posted the message
		 * @param username the name of the user who posted the message
		 * @param roomId the room ID
		 * @param messageId the message ID
		 * @return this
		 */
		public EventsResponseBuilder event(long timestamp, String content, int userId, String username, int roomId, long messageId) {
			if (!first) {
				sb.append(',');
			}
			first = false;

			//@formatter:off
			sb.append("{" +
				"\"event_type\": 1," +
				"\"time_stamp\": " + timestamp + "," +
				"\"content\": \"" + content + "\"," +
				"\"user_id\": " + userId + "," +
				"\"user_name\": \"" + username + "\"," +
				"\"room_id\": " + roomId + "," +
				"\"message_id\": " + messageId +
			"}");
			//@formatter:on

			return this;
		}

		/**
		 * Generates the final response string.
		 * @return the JSON response
		 */
		public String build() {
			return sb.toString() + "]}";
		}
	}

	/**
	 * Generates the kinds of messages that are received over the web socket
	 * connection.
	 * @return a builder object for building the messages
	 * @throws IOException
	 * @see https://github.com/JavaChat/OakBot/wiki/Example-WebSocket-Messages
	 */
	public static WebSocketMessageBuilder webSocket() throws IOException {
		return new WebSocketMessageBuilder();
	}

	public static class WebSocketMessageBuilder {
		private final StringWriter writer = new StringWriter();
		private final JsonGenerator generator;

		private int roomId;
		private String roomName;
		private boolean firstEvent = true, firstRoom = true;

		public WebSocketMessageBuilder() throws IOException {
			JsonFactory factory = new JsonFactory();
			generator = factory.createGenerator(writer);
			generator.setPrettyPrinter(new DefaultPrettyPrinter());

			generator.writeStartObject();
		}

		/**
		 * Sets what room the events are coming from. This method should be
		 * called before any other method.
		 * @param id
		 * @param name
		 * @return
		 * @throws IOException
		 */
		public WebSocketMessageBuilder room(int id, String name) throws IOException {
			roomId = id;
			roomName = name;

			if (!firstEvent) {
				generator.writeEndArray();
			}

			if (!firstRoom) {
				generator.writeEndObject();
			}
			firstRoom = false;

			generator.writeObjectFieldStart("r" + id);

			firstEvent = true;

			return this;
		}

		/**
		 * Adds a "new message" event.
		 * @param eventId
		 * @param timestamp
		 * @param content
		 * @param userId
		 * @param username
		 * @param messageId
		 * @return
		 * @throws IOException
		 */
		public WebSocketMessageBuilder newMessage(long eventId, long timestamp, String content, int userId, String username, long messageId) throws IOException {
			initEventList();

			generator.writeStartObject();
			generator.writeNumberField("event_type", 1);
			generator.writeNumberField("time_stamp", timestamp);
			generator.writeStringField("content", content);
			generator.writeNumberField("id", eventId);
			generator.writeNumberField("user_id", userId);
			generator.writeStringField("user_name", username);
			generator.writeNumberField("room_id", roomId);
			generator.writeStringField("room_name", roomName);
			generator.writeNumberField("message_id", messageId);
			generator.writeEndObject();

			return this;
		}

		private void initEventList() throws IOException {
			if (firstEvent) {
				generator.writeArrayFieldStart("e");
			}
			firstEvent = false;
		}

		public String build() throws IOException {
			if (!firstEvent) {
				generator.writeEndArray();
			}

			if (!firstRoom) {
				generator.writeEndObject();
			}

			generator.writeEndObject();

			generator.close();
			return writer.toString();
		}
	}

	private static String readFile(String file) throws IOException {
		URI uri;
		try {
			uri = ChatClientTest.class.getResource(file).toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		Path path = Paths.get(uri);
		return new String(Files.readAllBytes(path));
	}

	private ResponseSamples() {
		//hide
	}
}

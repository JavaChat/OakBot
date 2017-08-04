package oakbot.chat;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
	 * Generates the response for requesting the users that will receive
	 * notifications if they are mentioned.
	 * @return a builder object for building the response
	 */
	public static PingableUsersBuilder pingableUsers() {
		return new PingableUsersBuilder();
	}

	public static class PingableUsersBuilder {
		private final StringBuilder sb = new StringBuilder();
		private boolean first = true;

		public PingableUsersBuilder() {
			sb.append("[");
		}

		/**
		 * Adds a user to the response.
		 * @param userId the ID of the user
		 * @param username the name of the user
		 * @param unknown unknown
		 * @param lastMessage the last time they posted a message
		 * @return this
		 */
		public PingableUsersBuilder user(int userId, String username, long unknown, long lastMessage) {
			if (!first) {
				sb.append(',');
			}
			first = false;

			sb.append("[" + userId + ",\"" + username + "\"," + unknown + "," + lastMessage + "]");

			return this;
		}

		/**
		 * Generates the final response string.
		 * @return the JSON response
		 */
		public String build() {
			return sb.toString() + "]";
		}
	}

	/**
	 * Generates the response for requesting user info.
	 * @return a builder object for building the response
	 */
	public static UserInfoBuilder userInfo() {
		return new UserInfoBuilder();
	}

	public static class UserInfoBuilder {
		private final StringBuilder sb = new StringBuilder();
		private boolean first = true;

		public UserInfoBuilder() {
			sb.append("{\"users\":[");
		}

		public UserInfoBuilder user(int userId, String username, String emailHash, int reputation, boolean moderator, boolean owner, long lastPost, long lastSeen) {
			if (!first) {
				sb.append(',');
			}
			first = false;

			//@formatter:off
			sb.append("{" +
				"\"id\": " + userId + "," +
				"\"name\": \"" + username + "\"," +
				"\"email_hash\": \"" + emailHash + "\"," +
				"\"reputation\": " + reputation + "," +
				"\"is_moderator\": " + moderator + "," +
				"\"is_owner\": " + (owner ? "true" : "null") + "," +
				"\"last_post\": " + lastPost + "," +
				"\"last_seen\": " + lastSeen +
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
	 * Generates the response for getting room info.
	 * @param id the ID of the room
	 * @param name the name of the room
	 * @param description the room description
	 * @param favorite whether the user has marked the room as a favorite
	 * @param tags the room's tags
	 * @return the JSON response
	 */
	public static String roomInfo(int id, String name, String description, boolean favorite, List<String> tags) {
		StringBuilder tagsStr = new StringBuilder();
		boolean first = true;
		for (String tag : tags) {
			if (!first) {
				tagsStr.append(" ");
			}
			first = false;

			tagsStr.append("\\u003ca rel=\\\"noopener noreferrer\\\" class=\\\"tag\\\" href=\\\"http://stackoverflow.com/tags/" + tag + "/info\\\"\\u003e" + tag + "\\u003c/a\\u003e");
		}

		//@formatter:off
		return "{" +
			"\"id\": " + id + "," +
			"\"name\": \"" + name + "\"," +
			"\"description\": \"" + description + "\"," +
			"\"isFavorite\": " + favorite + "," +
			"\"usage\": null," +
			"\"tags\": \"" + tagsStr + "\"" +
		"}";
		//@formatter:on
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

	/**
	 * Creates a web socket message.
	 * @author Michael Angstadt
	 */
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
		 * @param id the room ID
		 * @param name the room name
		 * @return this
		 * @throws IOException never thrown
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
		 * Creates a "new message" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder newMessage() throws IOException {
			return new WsEventBuilder(1);
		}

		/**
		 * Creates a "message edited" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder messageEdited() throws IOException {
			return new WsEventBuilder(2);
		}

		/**
		 * Creates a "user entered the room" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder userEntered() throws IOException {
			return new WsEventBuilder(3);
		}

		/**
		 * Creates a "user left the room" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder userLeft() throws IOException {
			return new WsEventBuilder(4);
		}

		/**
		 * Creates a "message starred (or unstarred_" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder messageStarred() throws IOException {
			return new WsEventBuilder(6);
		}

		/**
		 * Creates a "user was mentioned" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder mention() throws IOException {
			return new WsEventBuilder(8);
		}

		/**
		 * Creates a "message deleted" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder messageDeleted() throws IOException {
			return new WsEventBuilder(10);
		}

		/**
		 * Creates a "reply posted" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder reply() throws IOException {
			return new WsEventBuilder(18);
		}

		/**
		 * Creates a "message was moved out of the room" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder movedOut() throws IOException {
			return new WsEventBuilder(19);
		}

		/**
		 * Creates a "message was moved into the room" event.
		 * @return the event builder
		 * @throws IOException never thrown
		 */
		public WsEventBuilder movedIn() throws IOException {
			return new WsEventBuilder(20);
		}

		/**
		 * Used for building individual events. An event is just a simple JSON
		 * object.
		 * @author Michael Angstadt
		 */
		public class WsEventBuilder {
			public WsEventBuilder(int eventType) throws IOException {
				if (firstEvent) {
					generator.writeArrayFieldStart("e");
				}
				firstEvent = false;

				generator.writeStartObject();
				eventType(eventType).room(roomId, roomName);
			}

			public WsEventBuilder eventType(int eventType) throws IOException {
				generator.writeNumberField("event_type", eventType);
				return this;
			}

			public WsEventBuilder timestamp(long timestamp) throws IOException {
				generator.writeNumberField("time_stamp", timestamp);
				return this;
			}

			public WsEventBuilder id(long id) throws IOException {
				generator.writeNumberField("id", id);
				return this;
			}

			public WsEventBuilder content(String content) throws IOException {
				generator.writeStringField("content", content);
				return this;
			}

			public WsEventBuilder user(int userId, String username) throws IOException {
				generator.writeNumberField("user_id", userId);
				generator.writeStringField("user_name", username);
				return this;
			}

			public WsEventBuilder targetUser(int userId) throws IOException {
				generator.writeNumberField("target_user_id", userId);
				return this;
			}

			public WsEventBuilder room(int roomId, String roomName) throws IOException {
				generator.writeNumberField("room_id", roomId);
				generator.writeStringField("room_name", roomName);
				return this;
			}

			public WsEventBuilder messageId(long messageId) throws IOException {
				generator.writeNumberField("message_id", messageId);
				return this;
			}

			public WsEventBuilder edits(int edits) throws IOException {
				generator.writeNumberField("message_edits", edits);
				return this;
			}

			public WsEventBuilder stars(int stars) throws IOException {
				generator.writeNumberField("message_stars", stars);
				return this;
			}

			public WsEventBuilder parentId(long parentId) throws IOException {
				generator.writeNumberField("parent_id", parentId);
				generator.writeBooleanField("show_parent", true);
				return this;
			}

			public WsEventBuilder moved() throws IOException {
				generator.writeBooleanField("moved", true);
				return this;
			}

			public WebSocketMessageBuilder done() throws IOException {
				generator.writeEndObject();
				return WebSocketMessageBuilder.this;
			}
		}

		/**
		 * Builds the final JSON object string.
		 * @return the JSON object string
		 * @throws IOException never thrown
		 */
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
			uri = ResponseSamples.class.getResource(file).toURI();
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

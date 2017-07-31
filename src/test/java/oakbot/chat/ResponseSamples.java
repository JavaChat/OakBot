package oakbot.chat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Allows you to create Stack Overflow Chat API response samples for unit
 * testing.
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
	public static EventsBuilder events() {
		return new EventsBuilder();
	}

	public static class EventsBuilder {
		private final StringBuilder sb = new StringBuilder();
		private boolean first = true;

		public EventsBuilder() {
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
		public EventsBuilder event(long timestamp, String content, int userId, String username, int roomId, long messageId) {
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

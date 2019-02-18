package oakbot.util;

import java.time.LocalDateTime;

import oakbot.bot.ChatCommand;
import oakbot.chat.ChatMessage;
import oakbot.command.Command;

/**
 * Builds mock {@link ChatCommand} objects for unit testing.
 * @author Michael Angstadt
 */
public class ChatCommandBuilder {
	private final String commandName;
	private int messageId, roomId, userId;
	private String trigger = "/", username, content = "";
	private LocalDateTime timestamp;

	/**
	 * @param command the command that is being tested
	 */
	public ChatCommandBuilder(Command command) {
		commandName = command.name();
	}

	/**
	 * Sets the command trigger.
	 * @param trigger the trigger (defaults to "/")
	 * @return this
	 */
	public ChatCommandBuilder trigger(String trigger) {
		this.trigger = trigger;
		return this;
	}

	/**
	 * Sets the message ID.
	 * @param messageId the message ID (defaults to 0)
	 * @return this
	 */
	public ChatCommandBuilder messageId(int messageId) {
		this.messageId = messageId;
		return this;
	}

	/**
	 * Sets the room ID.
	 * @param roomId the room ID (defaults to 0)
	 * @return this
	 */
	public ChatCommandBuilder roomId(int roomId) {
		this.roomId = roomId;
		return this;
	}

	/**
	 * Sets the user ID.
	 * @param userId the user ID (defaults to 0)
	 * @return this
	 */
	public ChatCommandBuilder userId(int userId) {
		this.userId = userId;
		return this;
	}

	/**
	 * Sets the username.
	 * @param username the username (defaults to null)
	 * @return this
	 */
	public ChatCommandBuilder username(String username) {
		this.username = username;
		return this;
	}

	/**
	 * Sets the timestamp.
	 * @param timestamp the timestamp (defaults to null)
	 * @return this
	 */
	public ChatCommandBuilder timestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	/**
	 * Sets the message content (excludes the command name).
	 * @param content the content (defaults to empty string)
	 * @return this
	 */
	public ChatCommandBuilder content(String content) {
		this.content = content;
		return this;
	}

	/**
	 * Constructs the mock {@link ChatCommand} object.
	 * @return the chat command
	 */
	public ChatCommand build() {
		//@formatter:off
		ChatMessage message = new ChatMessage.Builder()
			.messageId(messageId)
			.roomId(roomId)
			.userId(userId)
			.username(username)
			.timestamp(timestamp)
			.content(trigger + commandName + " " + content)
		.build();
		//@formatter:on

		return new ChatCommand(message, commandName, content);
	}
}

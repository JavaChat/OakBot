package oakbot.command.learn;

import static oakbot.bot.ChatActions.post;

import java.time.LocalDateTime;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.command.Command;
import oakbot.command.HelpDoc;

/**
 * A command that was taught to the bot at runtime using the "learn" command.
 * @author Michael Angstadt
 */
public class LearnedCommand implements Command {
	private final String authorUsername;
	private final Integer authorUserId, roomId;
	private final Long messageId;
	private final LocalDateTime created;
	private final String name, output;

	private LearnedCommand(Builder builder) {
		authorUsername = builder.authorUsername;
		authorUserId = builder.authorUserId;
		roomId = builder.roomId;
		messageId = builder.messageId;
		created = builder.created;
		name = builder.name;
		output = builder.output;
	}

	@Override
	public String name() {
		return name;
	}

	/**
	 * Gets the username of the person who created the command.
	 * @return the username or null if never recorded
	 */
	public String getAuthorUsername() {
		return authorUsername;
	}

	/**
	 * Gets the ID of the user who created the command.
	 * @return the user ID or null if never recorded
	 */
	public Integer getAuthorUserId() {
		return authorUserId;
	}

	/**
	 * Gets the ID of the room in which the command was created.
	 * @return roomId the room ID or null if never recorded
	 */
	public Integer getRoomId() {
		return roomId;
	}

	/**
	 * Gets the ID of the message which contains the "learn" command which
	 * created this command.
	 * @return messageId the message ID or null if never recorded
	 */
	public Long getMessageId() {
		return messageId;
	}

	/**
	 * Gets the time that the command was created.
	 * @return created the creation time or null if never recorded
	 */
	public LocalDateTime getCreated() {
		return created;
	}

	/**
	 * Gets the command output.
	 * @param output the output (will never be null)
	 * @return this
	 */
	public String getOutput() {
		return output;
	}

	@Override
	public HelpDoc help() {
		String summary;
		if (authorUsername == null) {
			summary = "This is a learned command that was created by chat room user. It does not have a help message.";
		} else {
			summary = "This is a learned command that was created by " + authorUsername + " (ID: " + authorUserId + ") at " + created + ". It does not have a help message.";
		}

		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary(summary)
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		return post(output);
	}

	/**
	 * Creates instances of the {@link LearnedCommand} class.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private String authorUsername;
		private Integer authorUserId, roomId;
		private Long messageId;
		private LocalDateTime created;
		private String name, output;

		/**
		 * Sets the username of the person who created the command.
		 * @param authorUsername the username
		 * @return this
		 */
		public Builder authorUsername(String authorUsername) {
			this.authorUsername = authorUsername;
			return this;
		}

		/**
		 * Sets the ID of the user who created the command.
		 * @param authorUserId the user ID
		 * @return this
		 */
		public Builder authorUserId(Integer authorUserId) {
			this.authorUserId = authorUserId;
			return this;
		}

		/**
		 * Sets the ID of the room in which the command was created.
		 * @param roomId the room ID
		 * @return this
		 */
		public Builder roomId(Integer roomId) {
			this.roomId = roomId;
			return this;
		}

		/**
		 * Sets the ID of the message which contains the "learn" command which
		 * created this command.
		 * @param messageId the message ID
		 * @return this
		 */
		public Builder messageId(Long messageId) {
			this.messageId = messageId;
			return this;
		}

		/**
		 * Sets the time that the command was created.
		 * @param created the creation time
		 * @return this
		 */
		public Builder created(LocalDateTime created) {
			this.created = created;
			return this;
		}

		/**
		 * Sets the name of the command.
		 * @param name the name
		 * @return this
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the command output.
		 * @param output the output
		 * @return this
		 */
		public Builder output(String output) {
			this.output = output;
			return this;
		}

		/**
		 * Builds the final {@link LearnedCommand} object.
		 * @return this
		 */
		public LearnedCommand build() {
			return new LearnedCommand(this);
		}
	}
}

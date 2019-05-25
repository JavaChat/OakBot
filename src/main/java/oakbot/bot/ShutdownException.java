package oakbot.bot;

import oakbot.command.Command;
import oakbot.listener.Listener;

/**
 * Thrown from a {@link Listener} or {@link Command} to terminate the bot.
 * @author Michael Angstadt
 */
@SuppressWarnings("serial")
public class ShutdownException extends RuntimeException {
	private final String message;
	private final boolean broadcast;

	/**
	 * @param message the message to post or null not to post anything
	 * @param broadcast true to broadcast the message to all rooms, false to
	 * only post the message to the room in which the command was invoked
	 */
	public ShutdownException(CharSequence message, boolean broadcast) {
		this.message = (message == null) ? null : message.toString();
		this.broadcast = broadcast;
	}

	/**
	 * Gets the message to post before the bot shuts down.
	 * @return the message or null not to post anything
	 */
	public String getShutdownMessage() {
		return message;
	}

	/**
	 * Determines whether the bot should broadcast the {@link getShutdownMessage
	 * message}.
	 * @return true to broadcast the message to all rooms, false to
	 * only post the message to the room in which the command was invoked
	 */
	public boolean isBroadcast() {
		return broadcast;
	}
}

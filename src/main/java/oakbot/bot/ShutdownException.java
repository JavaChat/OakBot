package oakbot.bot;

/**
 * Thrown from a {@link Command} to tell the bot to shut itself down.
 * @author Michael Angstadt
 */
@SuppressWarnings("serial")
public class ShutdownException extends RuntimeException {
	public ShutdownException(String message) {
		super(message);
	}
}

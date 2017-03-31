package oakbot.chat;

/**
 * Thrown when the login credentials for connecting to a chat system are bad.
 * @author Michael Angstadt
 * @see ChatConnection#login(String, String)
 */
@SuppressWarnings("serial")
public class InvalidCredentialsException extends RuntimeException {
	public InvalidCredentialsException() {
		super("Login credentials were rejected.");
	}
}

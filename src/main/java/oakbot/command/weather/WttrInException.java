package oakbot.command.weather;

/**
 * Thrown if the API returns an error response.
 * @author Michael Angstadt
 */
public class WttrInException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public WttrInException(String error) {
		super(error.trim()); //trim trailing new line
	}
}

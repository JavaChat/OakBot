package oakbot.command.stands4;

/**
 * Thrown when there is a problem doing a unit conversion.
 * @author Michael Angstadt
 * @see Stands4Client#convert
 */
public class ConvertException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final int code;

	public ConvertException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}

package oakbot.task;

/**
 * @author Michael Angstadt
 */
public class ChatGPTException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final String type, param, code;

	public ChatGPTException(String message, String type, String param, String code) {
		super(message);
		this.type = type;
		this.param = param;
		this.code = code;
	}

	public String getType() {
		return type;
	}

	public String getParam() {
		return param;
	}

	public String getCode() {
		return code;
	}
}

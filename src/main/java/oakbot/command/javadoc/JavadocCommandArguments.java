package oakbot.command.javadoc;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the arguments from a javadoc chat command.
 * @author Michael Angstadt
 */
class JavadocCommandArguments {
	private static final Pattern messageRegex = Pattern.compile("(.*?)(\\((.*?)\\))?(#(.*?)(\\((.*?)\\))?)?(\\s+(.*))?$");

	private final String className;
	private final String methodName;
	private final List<String> parameters;
	private final int paragraph;
	private final String targetUser;

	/**
	 * @param className the class name (may or may not be fully-qualified)
	 * @param methodName the method name or null if not defined
	 * @param parameters the method parameters or null if not defined
	 * @param paragraph the paragraph number
	 * @param targetUser the user that the user who posted the command wants the
	 * bot to ping when the bot posts the javadoc info or null if not specified
	 * (by default, the bot will reply to the user who posted the command)
	 */
	public JavadocCommandArguments(String className, String methodName, List<String> parameters, int paragraph, String targetUser) {
		this.className = className;
		this.methodName = methodName;
		this.parameters = parameters;
		this.paragraph = paragraph;
		this.targetUser = targetUser;
	}

	/**
	 * Parses the arguments out of a chat message.
	 * @param message the chat command
	 */
	public static JavadocCommandArguments parse(String message) {
		var m = messageRegex.matcher(message.trim());
		m.find();

		var className = m.group(1);
		var methodName = parseMethodName(m);
		var parameters = parseParameters(m);
		var paragraph = parseParagraph(m);
		var targetUser = parseTargetUser(m);

		return new JavadocCommandArguments(className, methodName, parameters, paragraph, targetUser);
	}

	private static String parseMethodName(Matcher m) {
		if (m.group(2) == null) {
			//e.g. java.lang.string#indexOf(int)
			return m.group(5);
		}

		//e.g. java.lang.string(string, string)
		var className = m.group(1);
		var dot = className.lastIndexOf('.');
		return (dot < 0) ? className : className.substring(dot + 1);
	}

	private static List<String> parseParameters(Matcher m) {
		var parametersStr = m.group(4); //e.g. java.lang.string(string, string)
		if (parametersStr == null || parametersStr.startsWith("#")) {
			parametersStr = m.group(7); //e.g. java.lang.string#string(string, string)
			if (parametersStr == null) {
				parametersStr = m.group(3);
			}
		}

		if (parametersStr == null || parametersStr.equals("*")) {
			return null;
		}

		if (parametersStr.isEmpty()) {
			return List.of();
		}

		return List.of(parametersStr.split("\\s*,\\s*"));
	}

	private static int parseParagraph(Matcher m) {
		var rest = m.group(9);
		if (rest == null || rest.isEmpty()) {
			return 1;
		}

		var split = rest.split("\\s+");
		var token = split[0];

		int num;
		try {
			num = Integer.parseInt(token);
		} catch (NumberFormatException e) {
			return 1;
		}

		return (num > 0) ? num : 1;
	}

	private static String parseTargetUser(Matcher m) {
		var rest = m.group(9);
		if (rest == null || rest.isEmpty()) {
			return null;
		}

		var split = rest.split("\\s+");
		var index = (split.length == 1) ? 0 : 1;
		var token = split[index];
		if (token.matches("-?\\d+")) {
			//paragraph number
			return null;
		}

		return (token.charAt(0) == '@') ? token.substring(1) : token;
	}

	/**
	 * Gets the class name.
	 * @return the class name (may or may not be fully-qualified)
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Gets the method name.
	 * @return the method name or null if not defined
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * Gets the method parameters
	 * @return the method parameters or null if not defined
	 */
	public List<String> getParameters() {
		return parameters;
	}

	/**
	 * Gets the paragraph number.
	 * @return the paragraph number
	 */
	public int getParagraph() {
		return paragraph;
	}

	/**
	 * Gets the user that the user who posted the command wants the bot to ping
	 * when the bot posts the javadoc info. By default, the bot will reply to
	 * the user who posted the command.
	 * @return the username or null if not specified
	 */
	public String getTargetUser() {
		return targetUser;
	}
}

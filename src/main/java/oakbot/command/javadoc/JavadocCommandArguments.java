package oakbot.command.javadoc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the arguments from a javadoc chat command.
 * @author Michael Angstadt
 */
class JavadocCommandArguments {
	private final static Pattern messageRegex = Pattern.compile("(.*?)(\\((.*?)\\))?(#(.*?)(\\((.*?)\\))?)?(\\s+(.*?))?$");

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
		Matcher m = messageRegex.matcher(message.trim());
		m.find();

		String className = m.group(1);

		String methodName;
		if (m.group(2) != null) { //e.g. java.lang.string(string, string)
			int dot = className.lastIndexOf('.');
			String simpleName = (dot < 0) ? className : className.substring(dot + 1);
			methodName = simpleName;
		} else {
			methodName = m.group(5); //e.g. java.lang.string#indexOf(int)
		}

		List<String> parameters;
		String parametersStr = m.group(4); //e.g. java.lang.string(string, string)
		if (parametersStr == null || parametersStr.startsWith("#")) {
			parametersStr = m.group(7); //e.g. java.lang.string#string(string, string)
			if (parametersStr == null) {
				parametersStr = m.group(3);
			}
		}
		if (parametersStr == null || parametersStr.equals("*")) {
			parameters = null;
		} else if (parametersStr.isEmpty()) {
			parameters = Collections.emptyList();
		} else {
			parameters = Arrays.asList(parametersStr.split("\\s*,\\s*"));
		}

		String rest = m.group(9);
		int paragraph = 1;
		String targetUser = null;
		if (rest != null && !rest.isEmpty()) {
			String split[] = rest.split("\\s+");
			if (split.length == 1) {
				String token = split[0];
				try {
					int i = Integer.parseInt(token);
					if (i > 0) {
						paragraph = i;
					}
				} catch (NumberFormatException e) {
					targetUser = (token.charAt(0) == '@') ? token.substring(1) : token;
				}
			} else {
				String token = split[0];
				try {
					int i = Integer.parseInt(token);
					if (i > 0) {
						paragraph = i;
					}
				} catch (NumberFormatException e) {
					//ignore
				}

				token = split[1];
				targetUser = (token.charAt(0) == '@') ? token.substring(1) : token;
			}
		}

		return new JavadocCommandArguments(className, methodName, parameters, paragraph, targetUser);
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

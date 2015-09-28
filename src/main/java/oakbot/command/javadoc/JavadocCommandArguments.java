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

	/**
	 * @param className the class name (may or may not be fully-qualified)
	 * @param methodName the method name or null if not defined
	 * @param parameters the method parameters or null if not defined
	 * @param paragraph the paragraph number
	 */
	public JavadocCommandArguments(String className, String methodName, List<String> parameters, int paragraph) {
		this.className = className;
		this.methodName = methodName;
		this.parameters = parameters;
		this.paragraph = paragraph;
	}

	/**
	 * Parses the arguments out of a chat message.
	 * @param message the chat command
	 */
	public static JavadocCommandArguments parse(String message) {
		Matcher m = messageRegex.matcher(message);
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

		int paragraph;
		try {
			paragraph = Integer.parseInt(m.group(9));
			if (paragraph < 1) {
				paragraph = 1;
			}
		} catch (NumberFormatException e) {
			paragraph = 1;
		}

		return new JavadocCommandArguments(className, methodName, parameters, paragraph);
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
}
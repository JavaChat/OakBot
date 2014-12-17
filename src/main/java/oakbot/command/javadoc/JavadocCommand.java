package oakbot.command.javadoc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command {
	private final JavadocDao dao = new JavadocDao();

	public void addLibrary(Path zipFile) throws IOException {
		dao.addApi(zipFile);
	}

	@Override
	public String name() {
		return "javadoc";
	}

	@Override
	public String description() {
		return "Displays class documentation from the Javadocs.";
	}

	@Override
	public String helpText() {
		return description(); //TODO finish
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		//parse the command
		CommandTextParser commandText = new CommandTextParser(message.getContent());

		ChatBuilder response;
		try {
			response = generateResponse(commandText.getClassName(), commandText.getMethodName(), commandText.getParameters(), commandText.getParagraph());
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		String reply = new ChatBuilder().reply(message).append(response).toString();
		return new ChatResponse(reply, SplitStrategy.WORD);
	}

	private ChatBuilder generateResponse(String className, String methodName, List<String> methodParams, int paragraph) throws IOException {
		ClassInfo info;
		try {
			info = dao.getClassInfo(className);
		} catch (MultipleClassesFoundException e) {
			ChatBuilder cb = new ChatBuilder();
			cb.append("Which one do you mean?");
			for (String name : e.getClasses()) {
				cb.nl().append("* ").append(name);
			}
			return cb;
		}

		if (info == null) {
			return new ChatBuilder("Sorry, I never heard of that class. :(");
		}

		ChatBuilder cb = new ChatBuilder();
		if (methodName == null) {
			if (paragraph == 1) {
				boolean deprecated = info.isDeprecated();
				for (String modifier : info.getModifiers()) {
					boolean italic = false;
					switch (modifier) {
					case "abstract":
					case "final":
						italic = true;
						break;
					case "class":
					case "enum":
					case "interface":
					case "exception":
						italic = false;
						break;
					case "@interface":
						italic = false;
						modifier = "annotation";
						break;
					default:
						//ignore all the rest
						continue;
					}

					if (italic) cb.italic();
					if (deprecated) cb.strike();
					cb.tag(modifier);
					if (deprecated) cb.strike();
					if (italic) cb.italic();
					cb.append(' ');
				}

				if (deprecated) cb.strike();
				String fullName = info.getName().getFull();
				String url = info.getUrl();
				if (url == null) {
					cb.bold().code(fullName).bold();
				} else {
					//TODO link directly to the method, not just the class
					cb.link(new ChatBuilder().bold().code(fullName).bold().toString(), url);
				}
				if (deprecated) cb.strike();
				cb.append(": ");
			}

			//get the class description
			String description = info.getDescription();
			String paragraphs[] = description.split("\n\n");
			if (paragraph > paragraphs.length) {
				paragraph = paragraphs.length;
			}
			String paragraphText = paragraphs[paragraph - 1];
			cb.append(paragraphText);
			if (paragraphs.length > 1) {
				cb.append(" (").append(paragraph + "").append("/").append(paragraphs.length + "").append(")");
			}
		} else {
			//find the method the user typed in
			MethodInfo matchingMethod = null;
			List<MethodInfo> matchingNames = new ArrayList<>();
			for (MethodInfo method : info.getMethods()) {
				if (!method.getName().equalsIgnoreCase(methodName)) {
					continue;
				}
				matchingNames.add(method);

				if (methodParams.size() != method.getParameters().size()) {
					continue;
				}

				boolean match = true;
				for (int i = 0; i < methodParams.size(); i++) {
					String param1 = methodParams.get(i);
					String param2 = method.getParameters().get(i).getType().getSimple();
					if (!param1.equalsIgnoreCase(param2)) {
						match = false;
						break;
					}
				}
				if (match) {
					matchingMethod = method;
					break;
				}
			}

			if (matchingMethod == null) {
				if (matchingNames.isEmpty()) {
					return cb.append("That method doesn't exist.");
				}

				if (matchingNames.size() > 1) {
					cb.append("Which one do you mean?");
					for (MethodInfo matchingName : matchingNames) {
						cb.nl().append("* #").append(matchingName.getName()).append('(');
						boolean first = true;
						for (ParameterInfo param : matchingName.getParameters()) {
							if (first) {
								first = false;
							} else {
								cb.append(", ");
							}
							cb.append(param.getType().getSimple());
						}
						cb.append(')');
					}
					return cb;
				}

				matchingMethod = matchingNames.get(0);
			}

			if (paragraph == 1) {
				boolean deprecated = matchingMethod.isDeprecated();
				for (String modifier : matchingMethod.getModifiers()) {
					if (deprecated) cb.strike();
					cb.tag(modifier);
					if (deprecated) cb.strike();
					cb.append(' ');
				}

				if (deprecated) cb.strike();
				String signature = matchingMethod.getSignatureString();
				String url = info.getUrl();
				if (url == null) {
					cb.bold().code(signature).bold();
				} else {
					cb.link(new ChatBuilder().bold().code(signature).bold().toString(), url);
				}
				if (deprecated) cb.strike();
				cb.append(": ");
			}

			//get the method description
			String description = matchingMethod.getDescription();
			String paragraphs[] = description.split("\n\n");
			if (paragraph > paragraphs.length) {
				paragraph = paragraphs.length;
			}
			String paragraphText = paragraphs[paragraph - 1];
			cb.append(paragraphText);
			if (paragraphs.length > 1) {
				cb.append(" (").append(paragraph + "").append("/").append(paragraphs.length + "").append(")");
			}
		}

		return cb;
	}

	static class CommandTextParser {
		private final static Pattern messageRegex = Pattern.compile("(.*?)(\\((.*?)\\))?(#(.*?)(\\((.*?)\\))?)?(\\s+(.*?))?$");

		private final String className, methodName;
		private final List<String> parameters;
		private final int paragraph;

		public CommandTextParser(String message) {
			Matcher m = messageRegex.matcher(message);
			m.find();

			className = m.group(1);

			if (m.group(2) != null) { //e.g. java.lang.string(string, string)
				int dot = className.lastIndexOf('.');
				String simpleName = (dot < 0) ? className : className.substring(dot + 1);
				methodName = simpleName;
			} else {
				methodName = m.group(5); //e.g. java.lang.string#indexOf(int)
			}

			String parametersStr = m.group(4); //e.g. java.lang.string(string, string)
			if (parametersStr == null || parametersStr.startsWith("#")) {
				parametersStr = m.group(7); //e.g. java.lang.string#string(string, string)
				if (parametersStr != null && parametersStr.isEmpty()) {
					parametersStr = null;
				}
				if (parametersStr == null) {
					parametersStr = m.group(3);
					if (parametersStr != null && parametersStr.isEmpty()) {
						parametersStr = null;
					}
				}
			}
			parameters = (parametersStr == null) ? Collections.emptyList() : Arrays.asList(parametersStr.split("\\s*,\\s*"));

			int paragraph;
			try {
				paragraph = Integer.parseInt(m.group(9));
				if (paragraph < 1) {
					paragraph = 1;
				}
			} catch (NumberFormatException e) {
				paragraph = 1;
			}
			this.paragraph = paragraph;
		}

		public String getClassName() {
			return className;
		}

		public String getMethodName() {
			return methodName;
		}

		public List<String> getParameters() {
			return parameters;
		}

		public int getParagraph() {
			return paragraph;
		}
	}
}

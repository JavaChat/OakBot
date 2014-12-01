package oakbot.javadoc;

import static oakbot.util.ChatUtils.reply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oakbot.bot.Command;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command {
	private final JavadocDao dao = new JavadocDao();
	private final Pattern regex = Pattern.compile("(.*?)#(.*?)(\\((.*?)\\)|$)");

	public void addLibrary(PageLoader loader, PageParser parser) throws IOException {
		dao.addJavadocApi(loader, parser);
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
	public String onMessage(ChatMessage message, boolean isAdmin) {
		String split[] = message.getContent().split("\\s+");

		String className = split[0].replaceAll("\\s", "");
		String methodName;
		List<String> methodParams;
		Matcher m = regex.matcher(className);
		if (m.find()) {
			className = m.group(1);
			methodName = m.group(2);

			String params = m.group(4);
			if (params == null || params.isEmpty()) {
				methodParams = Collections.emptyList();
			} else {
				String paramSplit[] = params.split(",");
				methodParams = new ArrayList<>(paramSplit.length);
				for (String param : paramSplit) {
					methodParams.add(param.toLowerCase());
				}
			}
		} else {
			methodName = null;
			methodParams = Collections.emptyList();
		}

		int paragraph;
		if (split.length == 1) {
			paragraph = 1;
		} else {
			try {
				paragraph = Integer.parseInt(split[1]);
				if (paragraph < 1) {
					paragraph = 1;
				}
			} catch (NumberFormatException e) {
				paragraph = 1;
			}
		}

		String response;
		try {
			response = generateResponse(className, methodName, methodParams, paragraph);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		return reply(message, response);
	}

	private String generateResponse(String className, String methodName, List<String> methodParams, int paragraph) throws IOException {
		ClassInfo info;
		try {
			info = dao.getClassInfo(className);
		} catch (MultipleClassesFoundException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Which one do you mean?");
			for (String name : e.getClasses()) {
				sb.append("\n* ").append(name);
			}
			return sb.toString();
		}

		if (info == null) {
			return "Sorry, I never heard of that class. :(";
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
				String fullName = info.getFullName();
				String url = info.getUrl();
				if (url == null) {
					cb.bold().code(fullName).bold();
				} else {
					cb.link(new ChatBuilder().bold().code(fullName).bold().toString(), url, "View the Javadocs");
				}
				if (deprecated) cb.strike();
				cb.append(": ");
			}

			//get the class description
			String description = info.getDescription();
			String split[] = description.split("\n\n");
			String paragraphText = (paragraph <= split.length) ? split[paragraph - 1] : split[split.length - 1];
			cb.append(paragraphText);
		} else {
			//find the method the user typed in
			MethodInfo methodInfo = null;
			for (MethodInfo method : info.getMethods()) {
				if (!method.getName().equalsIgnoreCase(methodName)) {
					continue;
				}

				if (methodParams.size() != method.getParameters().size()) {
					continue;
				}

				boolean match = true;
				for (int i = 0; i < methodParams.size(); i++) {
					String param1 = methodParams.get(i);
					String param2 = method.getParameters().get(i).getType();
					if (!param1.equalsIgnoreCase(param2)) {
						match = false;
						break;
					}
				}
				if (match) {
					methodInfo = method;
					break;
				}
			}

			if (methodInfo == null) {
				return "That method doesn't exist.";
			}

			if (paragraph == 1) {
				boolean deprecated = methodInfo.isDeprecated();
				for (String modifier : methodInfo.getModifiers()) {
					if (deprecated) cb.strike();
					cb.tag(modifier);
					if (deprecated) cb.strike();
					cb.append(' ');
				}

				if (deprecated) cb.strike();
				String signature = methodInfo.getSignatureString();
				String url = info.getUrl();
				if (url == null) {
					cb.bold().code(signature).bold();
				} else {
					cb.link(new ChatBuilder().bold().code(signature).bold().toString(), url, "View the Javadocs");
				}
				if (deprecated) cb.strike();
				cb.append(": ");
			}

			//get the method description
			String description = methodInfo.getDescription();
			String split[] = description.split("\n\n");
			String paragraphText = (paragraph <= split.length) ? split[paragraph - 1] : split[split.length - 1];
			cb.append(paragraphText);
		}

		return cb.toString();
	}
}

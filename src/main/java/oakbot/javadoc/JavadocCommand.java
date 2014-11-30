package oakbot.javadoc;

import static oakbot.util.ChatUtils.reply;

import java.io.IOException;

import oakbot.bot.Command;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * The command class for the chat bot.
 * @author Michael Angstadt
 */
public class JavadocCommand implements Command {
	private final JavadocDao dao = new JavadocDao();

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
		String className = split[0];

		int paragraph;
		if (split.length == 1) {
			paragraph = 1;
		} else {
			try {
				paragraph = Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				paragraph = 1;
			}
		}

		String response;
		try {
			response = generateResponse(className, paragraph);
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		return reply(message, response);
	}

	private String generateResponse(String commandText, int paragraph) throws IOException {
		if (paragraph < 1) {
			paragraph = 1;
		}

		ClassInfo info;
		try {
			info = dao.getClassInfo(commandText);
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

		return cb.toString();
	}
}

package oakbot.javadoc;

import java.io.IOException;

import oakbot.bot.Command;
import oakbot.chat.ChatMessage;

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
	public String onMessage(ChatMessage message) {
		String response;
		try {
			response = generateResponse(message.getContent());
		} catch (IOException e) {
			throw new RuntimeException("Problem getting Javadoc info.", e);
		}

		return ":" + message.getMessageId() + " " + response;
	}

	private String generateResponse(String commandText) throws IOException {
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

		StringBuilder sb = new StringBuilder();

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

			if (italic) sb.append('*');
			if (deprecated) sb.append("---");
			sb.append("[tag:").append(modifier).append("]");
			if (deprecated) sb.append("---");
			if (italic) sb.append('*');
			sb.append(' ');
		}

		if (deprecated) sb.append("---");
		String fullName = info.getFullName();
		String url = info.getUrl();
		if (url == null) {
			sb.append("**`").append(fullName).append("`**");
		} else {
			sb.append("[**`").append(fullName).append("`**](").append(url).append(" \"View the Javadocs\")");
		}
		if (deprecated) sb.append("---");
		sb.append(": ");

		//get the class description
		String description = info.getDescription();
		int pos = description.indexOf("\n");
		if (pos >= 0) {
			//just display the first paragraph
			description = description.substring(0, pos);
		}
		sb.append(description);

		return sb.toString();
	}
}

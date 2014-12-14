package oakbot.command.http;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;
import oakbot.util.DocumentWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Displays descriptions of HTTP response status codes.
 * @author Michael Angstadt
 */
public class HttpCommand implements Command {
	private final DocumentWrapper statusCodes;
	private final String url;

	public HttpCommand() {
		try (InputStream in = getClass().getResourceAsStream("status-codes.xml")) {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			statusCodes = new DocumentWrapper(document);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			//these should never be thrown because the XML is on the classpath
			throw new RuntimeException(e);
		}

		Element element = statusCodes.element("/statusCodes");
		if (element == null) {
			url = null;
		} else {
			String url = element.getAttribute("url");
			this.url = url.isEmpty() ? null : url;
		}
	}

	@Override
	public String name() {
		return "http";
	}

	@Override
	public String description() {
		return "Displays information about HTTP status codes and methods.";
	}

	@Override
	public String helpText() {
		return description();
	}

	@Override
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);

		String split[] = message.getContent().split("\\s+");
		String code = split[0];

		Element element = statusCodes.element("/statusCodes/statusCode[@code='" + code + "']");
		if (element == null) {
			cb.append("Status code not recognized.");
			return new ChatResponse(cb.toString());
		}

		int paragraph = 1;
		if (split.length > 1) {
			try {
				paragraph = Integer.parseInt(split[1]);
				if (paragraph < 1) {
					paragraph = 1;
				}
			} catch (NumberFormatException e) {
				paragraph = 1;
			}
		}

		if (paragraph == 1) {
			String name = element.getAttribute("name");
			ChatBuilder cb2 = new ChatBuilder();
			cb2.bold("HTTP " + code + " (" + name + ")");
			String linkText = cb2.toString();
			if (url == null) {
				cb.append(linkText);
			} else {
				String section = element.getAttribute("section");
				String url = this.url + (section.isEmpty() ? "" : "#section-" + section);
				cb.link(linkText, url);
			}
			cb.append(": ");
		}

		String description = element.getTextContent().trim();
		String paragraphText = getParagraph(description, paragraph);
		cb.append(paragraphText);

		return new ChatResponse(cb.toString(), SplitStrategy.WORD);
	}

	private static String getParagraph(String text, int num) {
		String split[] = text.split("\n\n");
		return (num <= split.length) ? split[num - 1] : split[split.length - 1];
	}
}

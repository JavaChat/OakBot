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
		try (InputStream in = getClass().getResourceAsStream("http.xml")) {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			statusCodes = new DocumentWrapper(document);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			//these should never be thrown because the XML is on the classpath
			throw new RuntimeException(e);
		}

		Element element = statusCodes.element("/http");
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
		String code = split[0].toUpperCase();
		if (code.isEmpty()) {
			cb.append("I need to know what status code (e.g. 200) or method (e.g. GET) you want to know about.");
			return new ChatResponse(cb.toString());
		}

		Element element = statusCodes.element("/http/statusCode[@code='" + code + "']");
		if (element == null) {
			element = statusCodes.element("/http/method[@name='" + code + "']");
			if (element == null) {
				String reply = code.matches("[0-9]+") ? "Status code not recognized." : "Method not recognized.";
				cb.append(reply);
				return new ChatResponse(cb.toString());
			}
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
			String linkText = "HTTP " + (element.getTagName().equals("statusCode") ? code + " (" + name + ")" : name);
			cb2.bold(linkText);
			if (url == null) {
				cb.append(cb2.toString());
			} else {
				String section = element.getAttribute("section");
				String url = this.url + (section.isEmpty() ? "" : "#section-" + section);
				cb.link(cb2.toString(), url);
			}
			cb.append(": ");
		}

		String description = element.getTextContent().trim();
		String paragraphs[] = description.split("\n\n");
		if (paragraph > paragraphs.length) {
			paragraph = paragraphs.length;
		}
		String paragraphText = paragraphs[paragraph - 1];
		cb.append(paragraphText);
		if (paragraphs.length > 1) {
			cb.append(" (").append(paragraph + "").append("/").append(paragraphs.length + "").append(")");
		}

		return new ChatResponse(cb.toString(), SplitStrategy.WORD);
	}
}

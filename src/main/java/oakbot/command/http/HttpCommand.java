package oakbot.command.http;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;
import oakbot.util.XPathWrapper;

/**
 * Displays descriptions of HTTP response status codes.
 * @author Michael Angstadt
 */
public class HttpCommand implements Command {
	private final Document document;
	private final XPathWrapper xpath = new XPathWrapper();

	public HttpCommand() {
		try (InputStream in = getClass().getResourceAsStream("http.xml")) {
			this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			//these should never be thrown because the XML is on the classpath
			throw new RuntimeException(e);
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
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Displays information about HTTP status codes and methods.  Descriptions come from the official RFC specifications.").nl()
			.append("Usage: ").append(trigger).append(name()).append(" METHOD|STATUS_CODE [PARAGRAPH=1]").nl()
			.append("Examples:").nl()
			.append(trigger).append(name()).append(" 200").nl()
			.append(trigger).append(name()).append(" GET").nl()
			.append(trigger).append(name()).append(" 200 2")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String split[] = chatCommand.getContent().split("\\s+");
		String code = split[0].toUpperCase();
		if (code.isEmpty()) {
			return reply("Tell me what status code (e.g. 200) or method (e.g. GET) you want to know about.", chatCommand);
		}

		boolean isStatusCode = true;
		Element element = xpath.element("/http/statusCode[@code='" + code + "']", document);
		if (element == null) {
			isStatusCode = false;
			element = xpath.element("/http/method[@name='" + code + "']", document);
			if (element == null) {
				String reply = code.matches("\\d+") ? "Status code not recognized." : "Method not recognized.";
				return reply(reply, chatCommand);
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

		String defaultRfc = getDefaultRfc(element);

		ChatBuilder cb = new ChatBuilder();
		cb.reply(chatCommand);
		if (paragraph == 1) {
			String name = element.getAttribute("name");
			String section = element.getAttribute("section");
			String url = rfcUrl(defaultRfc, section);

			ChatBuilder linkText = new ChatBuilder();
			linkText.bold().append("HTTP ");
			if (isStatusCode) {
				linkText.append(code).append(" (").append(name).append(')');
			} else {
				linkText.append(name);
			}
			linkText.bold();

			cb.link(linkText.toString(), url);
			cb.append(": ");
		}

		String description = element.getTextContent().trim();
		description = processSectionAnnotations(description, defaultRfc);

		String paragraphs[] = description.split("\n\n");
		if (paragraph > paragraphs.length) {
			paragraph = paragraphs.length;
		}
		String paragraphText = paragraphs[paragraph - 1];
		cb.append(paragraphText);
		if (paragraphs.length > 1) {
			cb.append(" (").append(paragraph).append("/").append(paragraphs.length).append(")");
		}

		return new ChatResponse(cb, SplitStrategy.WORD);
	}

	private static String processSectionAnnotations(String description, String defaultRfc) {
		Pattern p = Pattern.compile("#([\\d\\.]+)( (\\d+))?#");
		Matcher m = p.matcher(description);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String section = m.group(1);
			String rfc = m.group(3);

			String linkDisplay;
			if (section.matches("\\d{4}")) {
				rfc = section;
				section = null;
				linkDisplay = "RFC" + rfc;
			} else {
				StringBuilder display = new StringBuilder();
				display.append("Section " + section);
				if (rfc != null) {
					display.append(" of RFC").append(rfc);
				}
				linkDisplay = display.toString();
			}

			String linkRfc = (rfc == null) ? defaultRfc : rfc;

			ChatBuilder cb = new ChatBuilder();
			cb.link(linkDisplay, rfcUrl(linkRfc, section));
			m.appendReplacement(sb, cb.toString());
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String rfcUrl(String rfc, String section) {
		URIBuilder uri;
		try {
			uri = new URIBuilder("http://tools.ietf.org/html/rfc" + rfc);
		} catch (URISyntaxException e) {
			//should never be thrown
			throw new RuntimeException(e);
		}

		if (section != null && !section.isEmpty()) {
			uri.setFragment("section-" + section);
		}

		return uri.toString();
	}

	private static String getDefaultRfc(Element element) {
		while (true) {
			String rfc = element.getAttribute("rfc");
			if (!rfc.isEmpty()) {
				return rfc;
			}

			element = (Element) element.getParentNode();
			//"element" should never become null because the root <http> element has a "rfc" attribute
		}
	}
}

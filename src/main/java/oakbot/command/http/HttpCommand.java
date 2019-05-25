package oakbot.command.http;

import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.xml.sax.SAXException;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.Leaf;

/**
 * Displays descriptions of HTTP response status codes.
 * @author Michael Angstadt
 */
public class HttpCommand implements Command {
	private final Leaf document;

	public HttpCommand() {
		try (InputStream in = getClass().getResourceAsStream("http.xml")) {
			document = Leaf.parse(in);
		} catch (IOException | SAXException ignored) {
			/*
			 * These exceptions should never be thrown because the XML file is
			 * on the classpath and is not coming from user input.
			 * 
			 * TODO The XML file is also is checked for correctness in
			 * HttpCommandXMLTest.
			 */
			throw new RuntimeException(ignored);
		}
	}

	@Override
	public String name() {
		return "http";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays information about HTTP status codes and methods.")
			.detail("Descriptions come from the official RFC specifications.")
			.example("200", "Displays the description for the HTTP 200 status code.")
			.example("GET", "Displays the description for the HTTP GET method.")
			.example("200 2", "Displays paragraph 2 from the description of HTTP 200.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		String split[] = chatCommand.getContent().split("\\s+");
		String code = split[0].toUpperCase();
		if (code.isEmpty()) {
			return reply("Tell me what status code (e.g. 200) or method (e.g. GET) you want to know about.", chatCommand);
		}

		boolean isStatusCode = true;
		Leaf element = document.selectFirst("/http/statusCode[@code='" + code + "']");
		if (element == null) {
			isStatusCode = false;
			element = document.selectFirst("/http/method[@name='" + code + "']");
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
			String name = element.attribute("name");
			String section = element.attribute("section");
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

		String description = element.text().trim();
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

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb).splitStrategy(SplitStrategy.WORD)
		);
		//@formatter:on
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

	private static String getDefaultRfc(Leaf element) {
		while (true) {
			String rfc = element.attribute("rfc");
			if (!rfc.isEmpty()) {
				return rfc;
			}

			/*
			 * A null parent will never be returned become null because the root
			 * <http> element has a "rfc" attribute
			 */
			element = element.parent();
		}
	}
}

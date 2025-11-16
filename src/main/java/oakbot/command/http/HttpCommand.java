package oakbot.command.http;

import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.xml.sax.SAXException;

import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.util.Leaf;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays descriptions of HTTP response status codes.
 * @author Michael Angstadt
 */
public class HttpCommand implements Command {
	private final Leaf document;

	public HttpCommand() {
		try (var in = getClass().getResourceAsStream("http.xml")) {
			document = Leaf.parse(in);
		} catch (IOException | SAXException ignored) {
			/*
			 * These exceptions should never be thrown because the XML file is
			 * on the classpath and is not coming from user input. We know the
			 * XML file is valid because it is parsed in the unit test.
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
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var args = chatCommand.getContentAsArgs();
		if (args.isEmpty()) {
			return reply("Tell me what status code (e.g. 200) or method (e.g. GET) you want to know about.", chatCommand);
		}

		var code = args.get(0).toUpperCase();

		var element = document.selectFirst("/http/statusCode[@code='" + code + "']");
		var isStatusCode = (element != null);
		if (!isStatusCode) {
			element = document.selectFirst("/http/method[@name='" + code + "']");
			if (element == null) {
				var reply = code.matches("\\d+") ? "Status code not recognized." : "Method not recognized.";
				return reply(reply, chatCommand);
			}
		}

		var paragraph = getParagraph(args);
		var defaultRfc = getRfc(element);

		var cb = new ChatBuilder();
		if (paragraph == 1) {
			var name = element.attribute("name");
			var section = element.attribute("section");
			var url = rfcUrl(defaultRfc, section);

			var linkText = new ChatBuilder();
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

		var description = element.text().trim();
		description = processSectionAnnotations(description, defaultRfc);

		var paragraphs = description.split("\n\n");
		if (paragraph > paragraphs.length) {
			paragraph = paragraphs.length;
		}
		var paragraphText = paragraphs[paragraph - 1];
		cb.append(paragraphText);
		if (paragraphs.length > 1) {
			cb.append(" (").append(paragraph).append("/").append(paragraphs.length).append(")");
		}

		return reply(cb, chatCommand, SplitStrategy.WORD);
	}

	private static int getParagraph(List<String> args) {
		if (args.size() < 2) {
			return 1;
		}

		try {
			var paragraph = Integer.parseInt(args.get(1));
			return Math.max(paragraph, 1);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static String processSectionAnnotations(String description, String defaultRfc) {
		var p = Pattern.compile("#([\\d\\.]+)( (\\d+))?#");
		var m = p.matcher(description);
		var sb = new StringBuilder();
		while (m.find()) {
			var section = m.group(1);
			var rfc = m.group(3);

			String linkDisplay;
			if (section.matches("\\d{4}")) {
				rfc = section;
				section = null;
				linkDisplay = "RFC" + rfc;
			} else {
				var display = new StringBuilder();
				display.append("Section ").append(section);
				if (rfc != null) {
					display.append(" of RFC").append(rfc);
				}
				linkDisplay = display.toString();
			}

			var linkRfc = (rfc == null) ? defaultRfc : rfc;

			var cb = new ChatBuilder();
			cb.link(linkDisplay, rfcUrl(linkRfc, section));
			m.appendReplacement(sb, cb.toString());
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String rfcUrl(String rfc, String section) {
		//@formatter:off
		var uri = new URIBuilder()
			.setScheme("http")
			.setHost("tools.ietf.org")
			.setPathSegments("html", "rfc" + rfc);
		//@formatter:on

		if (section != null && !section.isEmpty()) {
			uri.setFragment("section-" + section);
		}

		return uri.toString();
	}

	private static String getRfc(Leaf element) {
		var rfc = element.attribute("rfc");
		return rfc.isEmpty() ? element.parent().attribute("rfc") : rfc;
	}
}

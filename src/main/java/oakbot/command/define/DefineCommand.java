package oakbot.command.define;

import static oakbot.command.Command.reply;
import static oakbot.util.XPathWrapper.children;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;
import oakbot.util.XPathWrapper;

/**
 * Gets word definitions from urbandictionary.com
 * @author Michael Angstadt
 */
public class DefineCommand implements Command {
	private static final Logger logger = Logger.getLogger(DefineCommand.class.getName());

	private final DocumentBuilder docBuilder;
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setIgnoringElementContentWhitespace(true);
		try {
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			//shouldn't be thrown
			throw new RuntimeException(e);
		}

		docBuilder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				//ignore
			}

			@Override
			public void error(SAXParseException exception) throws SAXException {
				//ignore
			}

			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				//ignore
			}
		});
	}

	private final XPathWrapper xpath = new XPathWrapper();
	private final String apiKey;

	public DefineCommand(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String name() {
		return "define";
	}

	@Override
	public String description() {
		return "Displays word definitions from the dictionary.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append("Displays word definitions from the dictionary.  ")
			.append("Definitions are retrieved from Merriam-Webster's dictionary API (http://www.dictionaryapi.com/).").nl()
			.append("Usage: ").append(trigger).append(name()).append(" WORD").nl()
			.append("Example: ").append(trigger).append(name()).append(" steganography")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String word = chatCommand.getContent().trim();
		if (word.isEmpty()) {
			return reply("Please specify the word you'd like to define.", chatCommand);
		}

		Document response;
		try {
			Escaper escaper = UrlEscapers.urlPathSegmentEscaper();
			URIBuilder b = new URIBuilder("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/" + escaper.escape(word));
			b.addParameter("key", apiKey);

			String url = b.toString();
			try (InputStream in = get(url)) {
				response = docBuilder.parse(in);
			}
		} catch (IOException | SAXException | URISyntaxException e) {
			logger.log(Level.SEVERE, "Problem getting word from dictionary.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Sorry, an unexpected error occurred while getting the definition: ")
				.code(e.getMessage())
			);
			//@formatter:on
		}

		List<Definition> definitions = parseResponse(word, response);
		if (definitions.isEmpty()) {
			List<String> suggestions = parseSuggestions(response);
			if (suggestions.isEmpty()) {
				return reply("No definitions found.", chatCommand);
			} else {
				return reply("No definitions found. Did you mean " + orList(suggestions) + "?", chatCommand);
			}
		}

		ChatBuilder cb = new ChatBuilder();
		for (Definition definition : definitions) {
			cb.append(word).append(" (").append(definition.getWordType()).append("):").nl();
			cb.append(definition.getDefinition());
			String example = definition.getExample();
			if (example != null) {
				cb.append(" (").append(definition.getExample()).append(")");
			}
			cb.nl().nl();
		}
		return new ChatResponse(cb.toString().trim(), SplitStrategy.NEWLINE);
	}

	private List<Definition> parseResponse(String word, Document response) {
		List<Definition> definitions = new ArrayList<>();
		for (Node entryNode : xpath.nodelist("/entry_list/entry", response)) {
			String ew = xpath.string("ew", entryNode);
			if (!word.equalsIgnoreCase(ew)) {
				//ignore similar words
				continue;
			}

			String type = xpath.string("fl", entryNode);
			for (Node dtNode : xpath.nodelist("def/dt", entryNode)) {
				String definitionText = getDefinition(dtNode);
				if (definitionText == null || definitionText.isEmpty()) {
					continue;
				}

				Definition definition = new Definition();
				definition.setWordType(type);
				definition.setDefinition(definitionText);
				definition.setExample(getExample(dtNode));

				definitions.add(definition);
			}
		}

		return definitions;
	}

	public String getDefinition(Node dtNode) {
		StringBuilder sb = new StringBuilder();
		for (Node child : children(dtNode)) {
			if (child instanceof Text) {
				sb.append(child.getTextContent());
				continue;
			}

			if (child instanceof Element) {
				Element element = (Element) child;
				switch (element.getNodeName()) {
				case "vi":
					/*
					 * Examples are parsed separately.
					 */
					continue;
				case "un":
					sb.append(":").append(getDefinition(element)).append(":");
					continue;
				}

				sb.append(element.getTextContent());
				continue;
			}
		}

		String split[] = sb.toString().split("\\s*:\\s*");
		List<String> list = new ArrayList<>();
		for (String s : split) {
			s = s.trim();
			if (!s.isEmpty()) {
				list.add(s);
			}
		}

		return String.join("; ", list);
	}

	private List<String> parseSuggestions(Document document) {
		Iterable<Element> elements = xpath.elements("/entry_list/suggestion", document);

		List<String> suggestions = new ArrayList<>();
		elements.forEach((e) -> suggestions.add(e.getTextContent()));
		return suggestions;
	}

	private static String orList(List<String> list) {
		if (list.size() == 1) {
			return list.get(0);
		}

		if (list.size() == 2) {
			return list.get(0) + " or " + list.get(1);
		}

		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String item : list) {
			if (i > 0) {
				sb.append(", ");
				if (i == list.size() - 1) {
					sb.append("or ");
				}
			}

			sb.append(item);
			i++;
		}
		return sb.toString();
	}

	public String getExample(Node dtNode) {
		Node viNode = xpath.node("vi", dtNode);
		if (viNode == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Node child : children(viNode)) {
			if ("aq".equals(child.getNodeName())) {
				//don't include the author of the example (for user-contributed content)
				continue;
			}
			sb.append(child.getTextContent());
		}
		return (sb.length() == 0) ? null : sb.toString().trim();
	}

	/**
	 * Makes an HTTP GET request to the given URL.
	 * @param url the URL
	 * @return the response body
	 * @throws IOException
	 */
	InputStream get(String url) throws IOException {
		URL urlObj = new URL(url);
		return urlObj.openStream();
	}
}
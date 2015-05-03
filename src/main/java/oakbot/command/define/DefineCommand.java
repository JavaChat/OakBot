package oakbot.command.define;

import static oakbot.util.XPathWrapper.it;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;
import oakbot.util.XPathWrapper;

import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
	public ChatResponse onMessage(ChatMessage message, boolean isAdmin) {
		String word = message.getContent().trim();
		if (word.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("You have to type a word to see its definition... -_-")
			);
			//@formatter:on
		}

		List<Definition> definitions;
		try {
			URIBuilder b = new URIBuilder("http://www.dictionaryapi.com/api/v1/references/collegiate/xml/" + URLEncoder.encode(word, "UTF-8"));
			b.addParameter("key", apiKey);
			String url = b.toString();

			try (InputStream response = get(url)) {
				definitions = parseResponse(word, response);
			}
		} catch (IOException | SAXException | URISyntaxException e) {
			logger.log(Level.SEVERE, "Problem getting word from dictionary.", e);

			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("Sorry, an unexpected error occurred getting the definition. >.>")
			);
			//@formatter:on
		}

		if (definitions.isEmpty()) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(message)
				.append("No definitions found.")
			);
			//@formatter:on
		}

		ChatBuilder cb = new ChatBuilder();
		cb.reply(message);
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

	private List<Definition> parseResponse(String word, InputStream in) throws SAXException, IOException {
		Document document = docBuilder.parse(in);
		List<Definition> definitions = new ArrayList<>();
		for (Node entryNode : xpath.nodelist("/entry_list/entry", document)) {
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
		String text = firstTextNode(dtNode);
		if (text == null) {
			return null;
		}

		String split[] = text.split("\\s*:\\s*");
		List<String> list = new ArrayList<>();
		for (String s : split) {
			s = s.trim();
			if (!s.isEmpty()) {
				list.add(s);
			}
		}

		return String.join("; ", list);
	}

	private String firstTextNode(Node node) {
		for (Node child : it(node.getChildNodes())) {
			if (child instanceof Text) {
				return child.getTextContent();
			}
		}
		return null;
	}

	public String getExample(Node dtNode) {
		Node viNode = xpath.node("vi", dtNode);
		if (viNode == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Node child : it(viNode.getChildNodes())) {
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
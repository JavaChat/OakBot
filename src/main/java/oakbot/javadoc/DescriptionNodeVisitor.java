package oakbot.javadoc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import oakbot.util.ChatBuilder;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

/**
 * Iterates through the class description section of a Javadoc HTML page,
 * converting the description to SO Chat markdown.
 * @author Michael Angstadt
 */
public class DescriptionNodeVisitor implements NodeVisitor {
	private final ChatBuilder cb = new ChatBuilder();
	private final Pattern escapeRegex = Pattern.compile("[*_\\[\\]]");
	private String prevText;

	private boolean inPre = false;
	private final StringBuilder preSb = new StringBuilder();

	private String linkUrl, linkTitle, linkText;
	private boolean linkTextCode;

	@Override
	public void head(Node node, int depth) {
		switch (node.nodeName()) {
		case "a":
			Element element = (Element) node;
			String href = element.absUrl("href");
			if (!href.isEmpty()) {
				linkUrl = href;
				linkTitle = element.attr("title");
			}
			break;
		case "code":
		case "tt":
			if (inLink()) {
				linkTextCode = true;
			} else {
				cb.code();
			}
			break;
		case "i":
		case "em":
			cb.italic();
			break;
		case "b":
		case "strong":
			cb.bold();
			break;
		case "br":
		case "p":
			cb.nl();
			if (!inPre) {
				cb.nl();
			}
			break;
		case "pre":
			inPre = true;
			cb.nl();
			break;
		case "#text":
			TextNode text = (TextNode) node;
			if (inPre) {
				preSb.append(text.getWholeText());
				break;
			}

			String content = text.text();
			content = escapeRegex.matcher(content).replaceAll("\\\\$0"); //escape special chars

			//in the jsoup javadocs, it's reading some text nodes twice for some reason
			//so, ignore the duplicate text nodes
			if (prevText != null && prevText.equals(content)) {
				prevText = null;
				break;
			}
			prevText = content;

			if (inLink()) {
				linkText = content;
			} else {
				cb.append(content);
			}

			break;
		}
	}

	@Override
	public void tail(Node node, int depth) {
		switch (node.nodeName()) {
		case "a":
			if (inLink()) {
				//"code" formatting has to be last
				ChatBuilder cb2 = new ChatBuilder();
				if (linkTextCode) {
					cb2.code(linkText);
				} else {
					cb2.append(linkText);
				}
				cb.link(cb2.toString(), linkUrl, linkTitle);

				linkUrl = linkText = linkTitle = null;
				linkTextCode = false;
			}
			break;
		case "code":
		case "tt":
			if (!inLink()) {
				cb.code();
			}
			break;
		case "i":
		case "em":
			cb.italic();
			break;
		case "b":
		case "strong":
			cb.bold();
			break;
		case "p":
			cb.nl();
			break;
		case "pre":
			inPre = false;
			handlePreText();
			cb.nl();
			preSb.setLength(0);
			break;
		}
	}

	private void handlePreText() {
		String text = preSb.toString().trim();
		String lines[] = text.split("\r\n|\n|\r");
		if (lines.length == 1) {
			cb.code(lines[0]).nl();
			return;
		}

		List<Integer> spaceCounts = new ArrayList<>(lines.length - 1);
		int minSpaces = Integer.MAX_VALUE;
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];

			//count the number of spaces/tabs at the beginning of the line
			int spaces = 0;
			for (int j = 0; j < line.length(); j++) {
				char c = line.charAt(j);
				if (c != ' ' && c != '\t') {
					break;
				}
				spaces++;
			}

			if (spaces < minSpaces) {
				minSpaces = spaces;
			}

			spaceCounts.add(spaces);
			lines[i] = line.trim();
		}

		cb.fixed().append(lines[0]).nl(); //handle the first line differently because its prepended spaces where trimmed
		Iterator<Integer> it = spaceCounts.iterator();
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			cb.fixed();
			int indent = it.next() - minSpaces;
			for (int j = 0; j < indent; j++) {
				cb.append(' ');
			}
			cb.append(line).nl();
		}
	}

	private boolean inLink() {
		return linkUrl != null;
	}

	/**
	 * Gets the description that was parsed.
	 * @return the description
	 */
	public String getDescription() {
		//@formatter:off
		return cb.toString()
		.trim()
		.replaceAll((char)160 + "", " ") //jsoup converts "&nbsp;" to a character that doesn't display right on SO Chat
		.replaceAll("[ \\t]+\\n", "\n") //remove whitespace that's at the end of a line
		.replaceAll("\\n{3,}", "\n\n"); //there should never be a run of more than 2 newlines
		//@formatter:on
	}
}
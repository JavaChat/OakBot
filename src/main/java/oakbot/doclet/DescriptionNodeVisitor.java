package oakbot.doclet;

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
 * Iterates through a class's or method's Javadoc description, converting the
 * description to SO Chat markdown.
 * @author Michael Angstadt
 */
public class DescriptionNodeVisitor implements NodeVisitor {
	private final ChatBuilder cb = new ChatBuilder();
	private final Pattern escapeRegex = Pattern.compile("[*_\\[\\]`]");
	private String prevText;

	private boolean inPre = false, inCode = false;
	private final StringBuilder preSb = new StringBuilder();

	private String linkUrl, linkTitle, linkText;
	private boolean linkTextCode, linkTextBold, linkTextItalic, linkTextStrike;

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
			} else if (!inPre) {
				inCode = true;
				cb.code();
			}
			break;
		case "i":
		case "em":
			if (inLink()) {
				linkTextItalic = true;
			} else if (!inPre) {
				cb.italic();
			}
			break;
		case "b":
		case "strong":
			if (inLink()) {
				linkTextBold = true;
			} else if (!inPre) {
				cb.bold();
			}
			break;
		case "strike":
		case "s":
		case "del":
			if (inLink()) {
				linkTextStrike = true;
			} else if (!inPre) {
				cb.strike();
			}
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
			cb.nl().nl();
			break;
		case "h1":
		case "h2":
		case "h3":
		case "h4":
		case "h5":
		case "h6":
			cb.nl().nl().bold();
			break;
		case "#text":
			TextNode text = (TextNode) node;
			if (inPre) {
				preSb.append(text.getWholeText());
				break;
			}

			String content = text.text();
			if (!inCode) {
				content = escapeRegex.matcher(content).replaceAll("\\\\$0"); //escape special chars
			}

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
				ChatBuilder cb2 = new ChatBuilder();
				if (linkTextBold) cb2.bold();
				if (linkTextItalic) cb2.italic();
				if (linkTextStrike) cb2.strike();
				if (linkTextCode) cb2.code(); //"code" formatting has to be last
				cb2.append(linkText);
				if (linkTextCode) cb2.code();
				if (linkTextStrike) cb2.strike();
				if (linkTextItalic) cb2.italic();
				if (linkTextBold) cb2.bold();

				cb.link(cb2.toString(), linkUrl, linkTitle);

				linkUrl = linkText = linkTitle = null;
				linkTextBold = linkTextItalic = linkTextStrike = linkTextCode = false;
			}
			break;
		case "code":
		case "tt":
			if (!inLink() && !inPre) {
				inCode = false;
				cb.code();
			}
			break;
		case "i":
		case "em":
			if (!inLink() && !inPre) {
				cb.italic();
			}
			break;
		case "b":
		case "strong":
			if (!inLink() && !inPre) {
				cb.bold();
			}
			break;
		case "strike":
		case "s":
		case "del":
			if (!inLink() && !inPre) {
				cb.strike();
			}
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
		case "h1":
		case "h2":
		case "h3":
		case "h4":
		case "h5":
		case "h6":
			cb.append(':').bold().append(' ');
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

		cb.fixed().append(lines[0]).nl(); //handle the first line differently because its prepended spaces were trimmed
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
		
		//jsoup converts "&nbsp;" to a character that doesn't display right on SO Chat
		.replace((char)160, ' ')
		
		//remove whitespace that's at the end of each line
		.replaceAll("[ \\t]+\\n", "\n")
		
		//there should never be a run of more than consecutive 2 newlines
		.replaceAll("\\n{3,}", "\n\n")
		
		//the code tag should be inner most formatting tag (e.g. "`**test**`" --> "**`test`**")
		.replaceAll("`([\\*\\-]+)(.*?)([\\*\\-]+)`", "$1`$2`$3")
		
		//move the code tags surrounding links so that they are inside the brackets (e.g. "`[test](...)`" --> "[`test`](...)")
		.replaceAll("`\\[(.*?)\\]\\((.*?)\\)`", "[`$1`]($2)")
		
		//run this regex again to fix certain edge cases with links (e.g. "`*[**test**](...)*`" --> "*`[**test**](...)`*" --> "*[**`test`**](...)*")
		.replaceAll("`([\\*\\-]+)(.*?)([\\*\\-]+)`", "$1`$2`$3");
		//@formatter:on
	}
}
package oakbot.javadoc;

import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

/**
 * Iterates through the description section of a class's Javadoc HTML page,
 * converting the description to SO Chat markdown.
 * @author Michael Angstadt
 */
public class DescriptionNodeVisitor implements NodeVisitor {
	private final StringBuilder sb = new StringBuilder();
	private final Pattern escapeRegex = Pattern.compile("[*_\\[\\]]");
	private boolean inPre = false;
	private String prevText;
	private String linkUrl, linkTitle, linkText;

	@Override
	public void head(Node node, int depth) {
		//for (int i = 0; i < depth; i++) {
		//	System.out.print(' ');
		//}
		//System.out.println("head " + node.nodeName());
		//if (node instanceof TextNode) {
		//	for (int i = 0; i < depth; i++) {
		//		System.out.print(' ');
		//	}
		//	System.out.println(((TextNode) node).text());
		//}

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
			sb.append("`");
			break;
		case "i":
		case "em":
			sb.append("*");
			break;
		case "b":
		case "strong":
			sb.append("**");
			break;
		case "br":
		case "p":
			sb.append("\n");
			break;
		case "pre":
			inPre = true;
			sb.append("\n");
			break;
		case "#text":
			TextNode text = (TextNode) node;
			String content;
			if (inPre) {
				content = text.getWholeText();
			} else {
				content = text.text();
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
				sb.append(content);
			}

			break;
		}
	}

	@Override
	public void tail(Node node, int depth) {
		//for (int i = 0; i < depth; i++) {
		//	System.out.print(' ');
		//}
		//System.out.println("tail " + node.nodeName());

		switch (node.nodeName()) {
		case "a":
			if (inLink()) {
				sb.append("[").append(linkText).append("](").append(linkUrl);
				if (!linkTitle.isEmpty()) {
					sb.append(" \"").append(linkTitle).append("\"");
				}
				sb.append(")");

				linkUrl = linkText = linkTitle = null;
			}
			break;
		case "code":
		case "tt":
			sb.append("`");
			break;
		case "i":
		case "em":
			sb.append("*");
			break;
		case "b":
		case "strong":
			sb.append("**");
			break;
		case "p":
			sb.append("\n");
			break;
		case "pre":
			inPre = false;
			sb.append("\n");
			break;
		}
	}

	private boolean inLink() {
		return linkUrl != null;
	}

	/**
	 * Gets the {@link StringBuilder} used to hold the description.
	 * @return the string builder
	 */
	public StringBuilder getStringBuilder() {
		return sb;
	}
}
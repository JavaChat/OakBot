package oakbot.javadoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/**
 * Parses the Java 8 Javadocs.
 * @author Michael Angstadt
 */
public class Java8PageParser implements PageParser {
	@Override
	public List<String> parseClassNames(Document document) {
		List<String> classNames = new ArrayList<>();
		for (Element element : document.select("ul li a")) {
			String url = element.attr("href");
			int dotPos = url.lastIndexOf('.');
			if (dotPos < 0) {
				continue;
			}

			url = url.substring(0, dotPos);
			url = url.replace('/', '.');
			classNames.add(url);
		}
		return classNames;
	}

	@Override
	public ClassInfo parseClassPage(Document document, String className) {
		String description;
		{
			Element descriptionElement = document.select(".block").first();
			DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
			descriptionElement.traverse(visitor);
			description = visitor.getDescription();
		}

		String url = getBaseUrl() + "?" + className.replace('.', '/') + ".html";

		boolean deprecated = false;
		List<String> modifiers;
		{
			Element element = document.select(".typeNameLabel").first();
			if (element == null) {
				//it might be an annotation
				element = document.select(".memberNameLabel").first();
			}
			TextNode textNode = (TextNode) element.siblingNodes().get(element.siblingIndex() - 1);

			//sometimes, other text comes before the modifiers on the previous line
			String text = textNode.getWholeText();
			int pos = text.lastIndexOf('\n');
			if (pos >= 0) {
				text = text.substring(pos + 1);
			}

			modifiers = Arrays.asList(text.trim().split(" "));

			//look for @Deprecated annotation
			Element parent = (Element) textNode.parent();
			for (Element child : parent.children()) {
				if ("@Deprecated".equals(child.text())) {
					deprecated = true;
					break;
				}
			}
		}

		return new ClassInfo(className, description, url, modifiers, deprecated);
	}

	@Override
	public String getBaseUrl() {
		return "https://docs.oracle.com/javase/8/docs/api/";
	}
}

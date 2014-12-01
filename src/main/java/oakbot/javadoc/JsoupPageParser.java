package oakbot.javadoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Parses the Jsoup Javadocs.
 * @author Michael Angstadt
 */
public class JsoupPageParser implements PageParser {
	@Override
	public List<String> parseClassNames(Document document) {
		List<String> classNames = new ArrayList<>();
		for (Element element : document.select("a")) {
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
			JsoupDescriptionNodeVisitor visitor = new JsoupDescriptionNodeVisitor();
			document.traverse(visitor);
			description = visitor.getDescription();
		}

		String url = getBaseUrl() + "?" + className.replace('.', '/') + ".html";

		List<String> modifiers;
		{
			Element element = document.select("dt pre").get(0);
			modifiers = Arrays.asList(element.text().trim().split(" "));
			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			int pos = modifiers.indexOf(simpleName);
			modifiers = modifiers.subList(0, pos);
		}

		return new ClassInfo(className, description, url, modifiers, Collections.emptyList(), false);
	}

	@Override
	public String getBaseUrl() {
		return "http://jsoup.org/apidocs/";
	}

	private static class JsoupDescriptionNodeVisitor extends DescriptionNodeVisitor {
		private Boolean inDescription;

		@Override
		public void head(Node node, int depth) {
			if (inDescription == Boolean.FALSE) {
				return;
			}

			if (inDescription == null) {
				if ("p".equals(node.nodeName())) {
					//the first <p> signals the start of the description
					inDescription = Boolean.TRUE;
				} else {
					return;
				}
			}

			if ("dl".equals(node.nodeName())) {
				inDescription = false;
				return;
			}

			super.head(node, depth);
		}

		@Override
		public void tail(Node node, int depth) {
			if (inDescription == Boolean.TRUE) {
				super.head(node, depth);
			}
		}
	}
}

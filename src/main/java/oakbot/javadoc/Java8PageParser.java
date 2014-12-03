package oakbot.javadoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

/**
 * Parses the Java 8 Javadocs.
 * @author Michael Angstadt
 */
public class Java8PageParser implements PageParser {
	private static final Logger logger = Logger.getLogger(Java8PageParser.class.getName());

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
		String description = parseDescription(document);

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

		List<MethodInfo> methods = parseConstructors(document);
		methods.addAll(parseMethods(document));

		return new ClassInfo(className, description, url, modifiers, methods, deprecated);
	}

	private String parseDescription(Document document) {
		Element descriptionElement = document.select(".block").first();
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		descriptionElement.traverse(visitor);
		return visitor.getDescription();
	}

	private List<MethodInfo> parseConstructors(Document document) {
		Element container = document.select("a[name=\"constructor.detail\"]").first();
		if (container == null) {
			return Collections.emptyList();
		}

		container = container.parent();

		List<MethodInfo> constructors = new ArrayList<>();
		for (Element element : container.select("ul.blockList li.blockList")) {
			Element preElement = element.select("pre").first();

			//@formatter:off
			String signature = preElement.text()
			.replace((char) 160, ' ') //remove "&nbsp;" characters
			.replaceAll("\\s{2,}", " ") //remove duplicate whitespace chars
			.trim(); //trim
			//@formatter:on

			boolean deprecated = signature.contains("@Deprecated");
			signature = signature.replace("@Deprecated", "").trim();
			logger.fine(signature);

			int startParen = signature.indexOf('(');
			int endParen = signature.lastIndexOf(')');

			List<String> modifiers;
			String name;
			{
				List<String> split = Arrays.asList(signature.substring(0, startParen).split("\\s+"));
				modifiers = split.subList(0, split.size() - 1);
				name = split.get(split.size() - 1);
			}

			List<MethodParameter> parameters;
			{
				String split[] = signature.substring(startParen + 1, endParen).replaceAll("<.*?>", "").split("[,\\s]+");
				if (split.length == 1) {
					parameters = Collections.emptyList();
				} else {
					parameters = new ArrayList<>(split.length / 2);
					for (int i = 0; i < split.length; i += 2) {
						String type = split[i];
						String variableName = split[i + 1];
						parameters.add(new MethodParameter(type, variableName));
					}
				}
			}

			String signatureString = name + signature.substring(startParen, endParen + 1);

			String description;
			{
				StringBuilder sb = new StringBuilder();
				for (Element div : element.select("div.block")) {
					DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
					div.traverse(visitor);
					if (sb.length() > 0) {
						sb.append("\n\n");
					}
					sb.append(visitor.getDescription());
				}
				description = sb.toString();
			}

			constructors.add(new MethodInfo(name, modifiers, parameters, description, signatureString, deprecated));
		}

		return constructors;
	}

	private List<MethodInfo> parseMethods(Document document) {
		Element container = document.select("a[name=\"method.detail\"]").first();
		if (container == null) {
			return Collections.emptyList();
		}

		container = container.parent();

		List<MethodInfo> methods = new ArrayList<>();
		for (Element element : container.select("ul.blockList li.blockList")) {
			Element preElement = element.select("pre").first();
			if (preElement == null) {
				continue;
			}

			String signature = cleanText(preElement.text());

			boolean deprecated = signature.contains("@Deprecated");
			signature = signature.replace("@Deprecated", "").trim();
			logger.fine(signature);

			int startParen = signature.indexOf('(');
			int endParen = signature.lastIndexOf(')');

			List<String> modifiers;
			String returnType;
			String name;
			{
				List<String> split = Arrays.asList(signature.substring(0, startParen).split("\\s+"));
				modifiers = split.subList(0, split.size() - 2);
				returnType = split.get(split.size() - 2);
				name = split.get(split.size() - 1);
			}

			List<MethodParameter> parameters;
			{
				String split[] = signature.substring(startParen + 1, endParen).replaceAll("<.*?>", "").split("[,\\s]+");
				if (split.length == 1) {
					parameters = Collections.emptyList();
				} else {
					parameters = new ArrayList<>(split.length / 2);
					for (int i = 0; i < split.length; i += 2) {
						String type = split[i];
						String variableName = split[i + 1];
						parameters.add(new MethodParameter(type, variableName));
					}
				}
			}

			String signatureString = returnType + " " + name + signature.substring(startParen, endParen + 1);

			String description;
			{
				StringBuilder sb = new StringBuilder();
				for (Element div : element.select("div.block")) {
					DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
					div.traverse(visitor);
					if (sb.length() > 0) {
						sb.append("\n\n");
					}
					sb.append(visitor.getDescription());
				}
				description = sb.toString();
			}

			methods.add(new MethodInfo(name, modifiers, parameters, description, signatureString, deprecated));
		}

		return methods;
	}

	private String cleanText(String text) {
		//@formatter:off
		return text
		.replace((char) 160, ' ') //remove "&nbsp;" characters
		.replaceAll("\\s{2,}", " ") //remove duplicate whitespace chars
		.trim(); //trim
		//@formatter:on
	}

	@Override
	public String getBaseUrl() {
		return "https://docs.oracle.com/javase/8/docs/api/";
	}
}

package oakbot.javadoc;

import java.util.Arrays;

import oakbot.util.DocumentWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parses {@link ClassInfo} objects from XML documents.
 * @author Michael Angstadt
 */
public class ClassInfoXmlParser {
	private final DocumentWrapper document;
	private final String baseUrl;

	/**
	 * @param document the XML document to parse
	 * @param baseUrl the library's base Javadoc URL
	 */
	public ClassInfoXmlParser(Document document, String baseUrl) {
		this.document = new DocumentWrapper(document);
		this.baseUrl = baseUrl;
	}

	/**
	 * Parses the {@link ClassInfo} object out of the XML data.
	 * @return the parse object
	 */
	public ClassInfo parse() {
		ClassInfo.Builder builder = new ClassInfo.Builder();

		//class name
		Element classElement = document.element("/class");
		String fullName = classElement.getAttribute("fullName");
		String simpleName = classElement.getAttribute("simpleName");
		builder.name(fullName, simpleName);

		//modifiers
		String value = classElement.getAttribute("modifiers");
		if (!value.isEmpty()) {
			builder.modifiers(Arrays.asList(value.split("\\s+")));
		}

		//super class
		value = classElement.getAttribute("extends");
		if (!value.isEmpty()) {
			builder.superClass(value);
		}

		//interfaces
		value = classElement.getAttribute("implements");
		if (!value.isEmpty()) {
			for (String full : Arrays.asList(value.split("\\s+"))) {
				builder.interface_(full);
			}
		}

		//deprecated
		value = classElement.getAttribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//description
		Element element = document.element("/class/description");
		if (element != null) {
			builder.description(element.getTextContent());
		}

		//constructors
		for (Element constructorElement : document.elements("/class/constructors/constructor")) {
			MethodInfo info = parseConstructor(constructorElement, simpleName);
			builder.method(info);
		}

		//methods
		for (Element methodElement : document.elements("/class/methods/method")) {
			MethodInfo method = parseMethod(methodElement);
			builder.method(method);
		}

		//URL
		String url = baseUrl + "?" + fullName.replace('.', '/') + ".html";
		builder.url(url);

		return builder.build();
	}

	private MethodInfo parseConstructor(Element element, String simpleName) {
		MethodInfo.Builder builder = new MethodInfo.Builder();

		//name
		builder.name(simpleName);

		//modifiers
		String value = element.getAttribute("modifiers");
		if (!value.isEmpty()) {
			builder.modifiers(Arrays.asList(value.split("\\s+")));
		}

		//description
		element = document.element("description", element);
		if (element != null) {
			builder.description(element.getTextContent());
		}

		//deprecated
		value = element.getAttribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//parameters
		for (Element parameterElement : document.elements("parameters/parameter", element)) {
			ParameterInfo parameter = parseParameter(parameterElement);
			builder.parameter(parameter);
		}

		return builder.build();
	}

	private MethodInfo parseMethod(Element element) {
		MethodInfo.Builder builder = new MethodInfo.Builder();

		//name
		builder.name(element.getAttribute("name"));

		//modifiers
		String value = element.getAttribute("modifiers");
		if (!value.isEmpty()) {
			builder.modifiers(Arrays.asList(value.split("\\s+")));
		}

		//description
		Element descriptionElement = document.element("description", element);
		if (descriptionElement != null) {
			builder.description(descriptionElement.getTextContent());
		}

		//return value
		value = element.getAttribute("returns");
		if (!value.isEmpty()) {
			builder.returnValue(new ClassName(value));
		}

		//deprecated
		value = element.getAttribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//parameters
		for (Element parameterElement : document.elements("parameters/parameter", element)) {
			ParameterInfo parameter = parseParameter(parameterElement);
			builder.parameter(parameter);
		}

		return builder.build();
	}

	private ParameterInfo parseParameter(Element element) {
		String type = element.getAttribute("type");

		//is it an array?
		boolean array = type.endsWith("[]");
		if (array) {
			type = type.substring(0, type.length() - 2);
		}

		//is a generic type? (like List<String>)
		int pos = type.indexOf('<');
		String generic = (pos < 0) ? null : type.substring(pos);
		if (generic != null) {
			type = type.substring(0, pos);
		}

		//name
		String name = element.getAttribute("name");

		return new ParameterInfo(new ClassName(type), name, array, generic);
	}
}

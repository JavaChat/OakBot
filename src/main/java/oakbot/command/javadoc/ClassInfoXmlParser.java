package oakbot.command.javadoc;

import java.util.Arrays;

import oakbot.util.XPathWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parses {@link ClassInfo} objects from XML documents.
 * @author Michael Angstadt
 */
public class ClassInfoXmlParser {
	private final Document document;
	private final XPathWrapper xpath = new XPathWrapper();
	private final LibraryZipFile zipFile;

	/**
	 * @param document the XML document to parse
	 * @param zipFile the ZIP file the class belongs to
	 */
	public ClassInfoXmlParser(Document document, LibraryZipFile zipFile) {
		this.document = document;
		this.zipFile = zipFile;
	}

	/**
	 * Parses the {@link ClassInfo} object out of the XML data.
	 * @return the parse object
	 */
	public ClassInfo parse() {
		ClassInfo.Builder builder = new ClassInfo.Builder();
		builder.zipFile(zipFile);

		//class name
		Element classElement = xpath.element("/class", document);
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
		Element element = xpath.element("/class/description", document);
		if (element != null) {
			builder.description(element.getTextContent());
		}

		//constructors
		for (Element constructorElement : xpath.elements("/class/constructors/constructor", document)) {
			MethodInfo info = parseConstructor(constructorElement, simpleName);
			builder.method(info);
		}

		//methods
		for (Element methodElement : xpath.elements("/class/methods/method", document)) {
			MethodInfo method = parseMethod(methodElement);
			builder.method(method);
		}

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
		Element descriptionElement = xpath.element("description", element);
		if (descriptionElement != null) {
			builder.description(descriptionElement.getTextContent());
		}

		//deprecated
		value = element.getAttribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//parameters
		for (Element parameterElement : xpath.elements("parameters/parameter", element)) {
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
		Element descriptionElement = xpath.element("description", element);
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
		for (Element parameterElement : xpath.elements("parameters/parameter", element)) {
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
		boolean varargs = type.endsWith("...");
		if (varargs) {
			type = type.substring(0, type.length() - 3);
		}

		//is a generic type? (like List<String>)
		int pos = type.indexOf('<');
		String generic = (pos < 0) ? null : type.substring(pos);
		if (generic != null) {
			type = type.substring(0, pos);
		}

		//name
		String name = element.getAttribute("name");

		return new ParameterInfo(new ClassName(type), name, array, varargs, generic);
	}
}

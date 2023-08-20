package oakbot.command.javadoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.mangstadt.sochat4j.util.Leaf;

/**
 * Parses {@link ClassInfo} objects from XML documents.
 * @author Michael Angstadt
 */
public class ClassInfoXmlParser {
	/**
	 * Parses a {@link ClassInfo} object out of the XML data.
	 * @param document the XML document
	 * @param zipFile the ZIP file the class belongs to
	 * @return the parsed information
	 * @throws IllegalArgumentException if the given XML document does not have
	 * a root {@literal <class>} element.
	 */
	public static ClassInfo parse(Leaf document, JavadocZipFile zipFile) {
		ClassInfoXmlParser parser = new ClassInfoXmlParser();
		ClassInfo.Builder builder = parser.parse(document);
		builder.zipFile(zipFile);
		return builder.build();
	}

	/**
	 * Parses a {@link ClassInfo} object from an XML document.
	 * @param document the XML document
	 * @return the parsed object
	 * @throws IllegalArgumentException if the given XML document does not have
	 * a root {@literal <class>} element.
	 */
	public ClassInfo.Builder parse(Leaf document) {
		ClassInfo.Builder builder = new ClassInfo.Builder();

		Leaf classElement = document.selectFirst("/class");
		if (classElement == null) {
			throw new IllegalArgumentException("XML file does not have a root <class> element.");
		}

		//class name
		ClassName className = parseClassName(classElement.attribute("name"));
		builder.name(className);

		//modifiers
		String value = classElement.attribute("modifiers");
		if (!value.isEmpty()) {
			builder.modifiers(List.of(value.split("\\s+")));
		}

		//super class
		value = classElement.attribute("extends");
		if (!value.isEmpty()) {
			builder.superClass(parseClassName(value));
		}

		//interfaces
		value = classElement.attribute("implements");
		if (!value.isEmpty()) {
			builder.interfaces(parseClassNames(value));
		}

		//deprecated
		value = classElement.attribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//since
		value = classElement.attribute("since");
		if (!value.isEmpty()) {
			builder.since(value);
		}

		//description
		Leaf element = document.selectFirst("/class/description");
		if (element != null) {
			builder.description(element.text());
		}

		//constructors
		for (Leaf constructorElement : document.select("/class/constructor")) {
			MethodInfo info = parseConstructor(constructorElement, className.getSimpleName());
			builder.method(info);
		}

		//methods
		for (Leaf methodElement : document.select("/class/method")) {
			MethodInfo method = parseMethod(methodElement);
			if (method != null) {
				builder.method(method);
			}
		}

		return builder;
	}

	/**
	 * Parses a {@link ClassName} object from the special format the XML file
	 * uses.
	 * @param value the string value from the XML file (e.g.
	 * "java.util|Map.Entry")
	 * @return the {@link ClassName} object
	 */
	private static ClassName parseClassName(String value) {
		int pipe = value.indexOf('|');
		String packageName = (pipe < 0) ? null : value.substring(0, pipe);

		String afterPipe = (pipe < 0) ? value : value.substring(pipe + 1);
		String[] split = afterPipe.split("\\.");
		List<String> outerClassNames = new ArrayList<>(split.length - 1);
		for (int i = 0; i < split.length - 1; i++) {
			outerClassNames.add(split[i]);
		}
		String simpleName = split[split.length - 1];

		return new ClassName(packageName, outerClassNames, simpleName);
	}

	/**
	 * Parses {@link ClassName} objects from the special class name format the
	 * XML file uses.
	 * @param value space-delimited string value from the XML file (e.g.
	 * "java.util|Map.Entry java.lang|String")
	 * @return the {@link ClassName} objects
	 */
	private static List<ClassName> parseClassNames(String value) {
		String[] split = value.trim().split("\\s+");
		return Arrays.stream(split).map(ClassInfoXmlParser::parseClassName).collect(Collectors.toList());
	}

	private MethodInfo parseConstructor(Leaf element, String simpleName) {
		MethodInfo.Builder builder = new MethodInfo.Builder();

		//name
		builder.name(simpleName);

		//modifiers
		String value = element.attribute("modifiers");
		if (!value.isEmpty()) {
			builder.modifiers(List.of(value.split("\\s+")));
		}

		//since
		value = element.attribute("since");
		if (!value.isEmpty()) {
			builder.since(value);
		}

		//description
		Leaf descriptionElement = element.selectFirst("description");
		if (descriptionElement != null) {
			builder.description(descriptionElement.text());
		}

		//deprecated
		value = element.attribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//parameters
		for (Leaf parameterElement : element.select("parameter")) {
			ParameterInfo parameter = parseParameter(parameterElement);
			builder.parameter(parameter);
		}

		return builder.build();
	}

	private MethodInfo parseMethod(Leaf element) {
		MethodInfo.Builder builder = new MethodInfo.Builder();

		//name
		String name = element.attribute("name");
		if (name.isEmpty()) {
			return null;
		}
		builder.name(name);

		//modifiers
		String value = element.attribute("modifiers");
		if (!value.isEmpty()) {
			builder.modifiers(List.of(value.split("\\s+")));
		}

		//since
		value = element.attribute("since");
		if (!value.isEmpty()) {
			builder.since(value);
		}

		//description
		Leaf descriptionElement = element.selectFirst("description");
		if (descriptionElement != null) {
			builder.description(descriptionElement.text());
		}

		//return value
		value = element.attribute("returns");
		if (!value.isEmpty()) {
			builder.returnValue(parseClassName(value));
		}

		//deprecated
		value = element.attribute("deprecated");
		builder.deprecated(value.isEmpty() ? false : Boolean.parseBoolean(value));

		//parameters
		for (Leaf parameterElement : element.select("parameter")) {
			ParameterInfo parameter = parseParameter(parameterElement);
			if (parameter != null) {
				builder.parameter(parameter);
			}
		}

		return builder.build();
	}

	private ParameterInfo parseParameter(Leaf element) {
		//type
		String type = element.attribute("type");

		//is it an array?
		boolean array = type.endsWith("[]");
		if (array) {
			type = type.substring(0, type.length() - 2);
		}

		//is it varargs?
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

		ClassName className = parseClassName(type);

		//name
		String name = element.attribute("name");

		return new ParameterInfo(className, name, array, varargs, generic);
	}
}

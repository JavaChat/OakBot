package oakbot.command.javadoc;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
		var parser = new ClassInfoXmlParser();
		var builder = parser.parse(document);
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
		var builder = new ClassInfo.Builder();

		var classElement = document.selectFirst("/class");
		if (classElement == null) {
			throw new IllegalArgumentException("XML file does not have a root <class> element.");
		}

		//class name
		var className = parseClassName(classElement.attribute("name"));
		builder.name(className);

		//modifiers
		var value = classElement.attribute("modifiers");
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
		var element = document.selectFirst("/class/description");
		if (element != null) {
			builder.description(element.text());
		}

		//constructors
		//@formatter:off
		document.select("/class/constructor").stream()
			.map(constructorElement -> parseConstructor(constructorElement, className.getSimpleName()))
		.forEach(builder::method);
		//@formatter:on

		//methods
		//@formatter:off
		document.select("/class/method").stream()
			.map(this::parseMethod)
			.filter(Objects::nonNull)
		.forEach(builder::method);
		//@formatter:on

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
		var pipe = value.indexOf('|');
		var packageName = (pipe < 0) ? null : value.substring(0, pipe);

		var afterPipe = (pipe < 0) ? value : value.substring(pipe + 1);
		var split = afterPipe.split("\\.");
		var outerClassNames = Arrays.asList(split).subList(0, split.length - 1);
		var simpleName = split[split.length - 1];

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
		var split = value.trim().split("\\s+");
		//@formatter:off
		return Arrays.stream(split)
			.map(ClassInfoXmlParser::parseClassName)
		.toList();
		//@formatter:on
	}

	private MethodInfo parseConstructor(Leaf element, String simpleName) {
		var builder = new MethodInfo.Builder();

		//name
		builder.name(simpleName);

		//modifiers
		var value = element.attribute("modifiers");
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
		//@formatter:off
		element.select("parameter").stream()
			.map(this::parseParameter)
		.forEach(builder::parameter);
		//@formatter:on

		return builder.build();
	}

	private MethodInfo parseMethod(Leaf element) {
		var builder = new MethodInfo.Builder();

		//name
		var name = element.attribute("name");
		if (name.isEmpty()) {
			return null;
		}
		builder.name(name);

		//modifiers
		var value = element.attribute("modifiers");
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
		//@formatter:off
		element.select("parameter").stream()
			.map(this::parseParameter)
		.forEach(builder::parameter);
		//@formatter:on

		return builder.build();
	}

	private ParameterInfo parseParameter(Leaf element) {
		//type
		var type = element.attribute("type");

		//is it an array?
		var array = type.endsWith("[]");
		if (array) {
			type = type.substring(0, type.length() - 2);
		}

		//is it varargs?
		var varargs = type.endsWith("...");
		if (varargs) {
			type = type.substring(0, type.length() - 3);
		}

		//is a generic type? (like List<String>)
		var pos = type.indexOf('<');
		var generic = (pos < 0) ? null : type.substring(pos);
		if (generic != null) {
			type = type.substring(0, pos);
		}

		var className = parseClassName(type);

		//name
		var name = element.attribute("name");

		return new ParameterInfo(className, name, array, varargs, generic);
	}
}

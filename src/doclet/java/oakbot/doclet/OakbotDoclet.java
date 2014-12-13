package oakbot.doclet;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oakbot.javadoc.DescriptionNodeVisitor;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * A custom Javadoc doclet that saves class information to XML files inside of a
 * ZIP file.
 * @author Michael Angstadt
 */
public class OakbotDoclet {
	private static final Path outputFile = Paths.get("java8-xml.zip");
	private static final String libraryName = "Java 8";
	private static final String baseUrl = "https://docs.oracle.com/javase/8/docs/api/";
	private static final boolean indentXml = false;

	/**
	 * The entry point for the {@code javadoc} command.
	 * @param rootDoc the root
	 * @return true if successful, false if not
	 * @throws Exception if an error occurred during the parsing
	 */
	public static boolean start(RootDoc rootDoc) throws Exception {
		System.out.println("Building ZIP file...");

		if (Files.exists(outputFile)) {
			Files.delete(outputFile);
		}

		URI uri = URI.create("jar:file:" + outputFile.toAbsolutePath());
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
			writeInfoDocument(fs);

			for (ClassDoc classDoc : rootDoc.classes()) {
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				document.appendChild(parseClass(classDoc, document));

				Path path = fs.getPath(classDoc.qualifiedTypeName() + ".xml");
				try (Writer writer = Files.newBufferedWriter(path)) {
					writeDocument(document, writer);
				}
			}
		}

		return true;
	}

	private static void writeInfoDocument(FileSystem fs) throws IOException, TransformerException, ParserConfigurationException {
		//create the XML DOM
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element element = document.createElement("info");
		element.setAttribute("name", libraryName);
		element.setAttribute("baseUrl", baseUrl);
		document.appendChild(element);

		//write it to a file
		Path info = fs.getPath("info.xml");
		try (Writer writer = Files.newBufferedWriter(info)) {
			writeDocument(document, writer);
		}
	}

	private static Element parseClass(ClassDoc classDoc, Document document) {
		Element element = document.createElement("class");

		//full name
		String fullName = classDoc.qualifiedTypeName();
		element.setAttribute("fullName", fullName);

		//simple name
		String simpleName = classDoc.simpleTypeName();
		element.setAttribute("simpleName", simpleName);

		//modifiers
		StringBuilder sb = new StringBuilder();
		if (classDoc.isException()) {
			sb.append("exception ");
		} else if (classDoc.isEnum()) {
			sb.append("enum ");
		} else if (classDoc.isClass()) {
			sb.append("class ");
		}
		//note: "interface" is already included in the modifiers for interfaces
		//TODO how to determine if it's an annotation?
		sb.append(classDoc.modifiers());
		element.setAttribute("modifiers", sb.toString().trim());

		//super class
		ClassDoc superClass = classDoc.superclass();
		if (superClass != null) {
			element.setAttribute("extends", superClass.qualifiedTypeName());
		}

		//interfaces
		List<String> implementsList = new ArrayList<>();
		for (ClassDoc interfaceDoc : classDoc.interfaces()) {
			implementsList.add(interfaceDoc.qualifiedTypeName());
		}
		if (!implementsList.isEmpty()) {
			element.setAttribute("implements", String.join(" ", implementsList));
		}

		//deprecated
		if (isDeprecated(classDoc)) {
			element.setAttribute("deprecated", "true");
		}

		//description
		String description = toMarkdown(classDoc.inlineTags());
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//constructors
		Element constructorsElement = document.createElement("constructors");
		for (ConstructorDoc constructor : classDoc.constructors()) {
			constructorsElement.appendChild(parseConstructor(constructor, document));
		}
		element.appendChild(constructorsElement);

		//methods
		Element methodsElement = document.createElement("methods");
		for (MethodDoc method : classDoc.methods()) {
			methodsElement.appendChild(parseMethod(method, document));
		}
		element.appendChild(methodsElement);

		return element;
	}

	private static Element parseConstructor(ConstructorDoc constructor, Document document) {
		Element element = document.createElement("constructor");

		//deprecated
		if (isDeprecated(constructor)) {
			element.setAttribute("deprecated", "true");
		}

		//thrown exceptions
		List<String> exceptions = new ArrayList<>();
		for (Type type : constructor.thrownExceptionTypes()) {
			exceptions.add(type.qualifiedTypeName());
		}
		if (!exceptions.isEmpty()) {
			element.setAttribute("throws", String.join(" ", exceptions));
		}

		//description
		String description = toMarkdown(constructor.inlineTags());
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//parameters
		Element parametersElement = document.createElement("parameters");
		for (Parameter parameter : constructor.parameters()) {
			parametersElement.appendChild(parseParameter(parameter, document));
		}
		element.appendChild(parametersElement);

		return element;
	}

	private static Element parseMethod(MethodDoc method, Document document) {
		Element element = document.createElement("method");

		//name
		String name = method.name();
		element.setAttribute("name", name);

		//modifiers
		String modifiers = method.modifiers();
		element.setAttribute("modifiers", modifiers);

		//deprecated
		if (isDeprecated(method)) {
			element.setAttribute("deprecated", "true");
		}

		//return value
		String returns = method.returnType().qualifiedTypeName();
		element.setAttribute("returns", returns);

		//thrown exceptions
		List<String> exceptions = new ArrayList<>();
		for (Type type : method.thrownExceptionTypes()) {
			exceptions.add(type.qualifiedTypeName());
		}
		if (!exceptions.isEmpty()) {
			element.setAttribute("throws", String.join(" ", exceptions));
		}

		//description
		String description;
		MethodDoc overriddenMethod = findOverriddenMethod(method);
		if (overriddenMethod != null) {
			if (overriddenMethod.containingClass().isPackagePrivate()) {
				description = toMarkdown(overriddenMethod.inlineTags());
			} else {
				String qualifiedName = overriddenMethod.qualifiedName();
				int pos = qualifiedName.lastIndexOf('.');
				qualifiedName = qualifiedName.substring(0, pos) + "#" + qualifiedName.substring(pos + 1);

				//e.g. oakbot.javadoc.PageParser#parseClassPage(org.jsoup.nodes.Document, java.lang.String)
				element.setAttribute("overrides", qualifiedName + overriddenMethod.signature());

				description = toMarkdown(method.inlineTags());
			}
		} else {
			description = toMarkdown(method.inlineTags());
		}
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//parameters
		Element parametersElement = document.createElement("parameters");
		for (Parameter parameter : method.parameters()) {
			parametersElement.appendChild(parseParameter(parameter, document));
		}
		element.appendChild(parametersElement);

		return element;
	}

	private static Element parseParameter(Parameter parameter, Document document) {
		Element element = document.createElement("parameter");

		String name = parameter.name();
		element.setAttribute("name", name);

		String type = parameter.type().qualifiedTypeName();
		element.setAttribute("type", type + parameter.type().dimension());

		return element;
	}

	private static boolean isDeprecated(ProgramElementDoc element) {
		for (AnnotationDesc annotation : element.annotations()) {
			if ("Deprecated".equals(annotation.annotationType().simpleTypeName())) {
				return true;
			}
		}
		return false;
	}

	private static MethodDoc findOverriddenMethod(MethodDoc method) {
		MethodDoc overriddenMethod = method.overriddenMethod();
		if (overriddenMethod != null) {
			return overriddenMethod;
		}

		Parameter[] methodParams = method.parameters();
		for (ClassDoc interfaceDoc : method.containingClass().interfaces()) {
			for (MethodDoc interfaceMethod : interfaceDoc.methods()) {
				if (!interfaceMethod.name().equals(method.name())) {
					continue;
				}

				Parameter[] interfaceMethodParams = interfaceMethod.parameters();
				if (methodParams.length != interfaceMethodParams.length) {
					continue;
				}

				boolean matches = true;
				for (int i = 0; i < methodParams.length; i++) {
					Parameter one = methodParams[i];
					Parameter two = interfaceMethodParams[i];
					if (!one.type().qualifiedTypeName().equals(two.type().qualifiedTypeName())) {
						matches = false;
						break;
					}
				}

				if (matches) {
					return interfaceMethod;
				}
			}
		}
		return null;
	}

	private static String toMarkdown(Tag inlineTags[]) {
		StringBuilder sb = new StringBuilder();
		for (Tag tag : inlineTags) {
			String text = tag.text();
			switch (tag.name()) {
			case "@code":
			case "@link":
				sb.append("<code>").append(text).append("</code>");
				break;
			default:
				sb.append(text);
				break;
			}
		}

		org.jsoup.nodes.Document document = Jsoup.parse(sb.toString());
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);
		return visitor.getDescription();
	}

	private static void writeDocument(Node node, Writer writer) throws TransformerException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		if (indentXml) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		}

		DOMSource source = new DOMSource(node);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
	}
}

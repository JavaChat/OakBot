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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
	/**
	 * The name of the ZIP file to create.
	 */
	private static final Path outputPath;

	/**
	 * The name of the library.
	 */
	private static final String libraryName;
	
	/**
	 * The version of the library.
	 */
	private static final String libraryVersion;

	/**
	 * The URL to the library's Javadocs.
	 */
	private static final String baseUrl;
	
	/**
	 * The URL to the library's webpage.
	 */
	private static final String projectUrl;

	/**
	 * Whether or not to pretty print the XML.
	 */
	private static final boolean prettyPrint;

	static {
		ConfigProperties properties = new ConfigProperties(System.getProperties());

		Path path = properties.getOutputPath();
		outputPath = (path == null) ? Paths.get("javadocs.zip") : path;

		prettyPrint = properties.isPrettyPrint();

		libraryName = properties.getLibraryName();
		
		libraryVersion = properties.getLibraryVersion();
		
		projectUrl = properties.getProjectUrl();

		String url = properties.getLibraryBaseUrl();
		if (url != null && !url.endsWith("/")) {
			url += "/";
		}
		baseUrl = url;
	}

	/**
	 * The entry point for the {@code javadoc} command.
	 * @param rootDoc contains the parsed javadoc information
	 * @return true if successful, false if not
	 * @throws Exception if an error occurred during the parsing
	 */
	public static boolean start(RootDoc rootDoc) throws Exception {
		System.out.println("OakBot Doclet");
		System.out.println("Saving to: " + outputPath);

		if (Files.exists(outputPath)) {
			Files.delete(outputPath);
		}

		URI uri = URI.create("jar:file:" + outputPath.toAbsolutePath());
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
			writeInfoDocument(fs);

			ClassDoc classDocs[] = rootDoc.classes();
			StatusPrinter printer = new StatusPrinter(classDocs.length);
			for (ClassDoc classDoc : classDocs) {
				//output status to console
				printer.print(classDoc);

				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element classElement = parseClass(classDoc, document);
				document.appendChild(classElement);

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
		element.setAttribute("version", libraryVersion);
		element.setAttribute("baseUrl", baseUrl);
		element.setAttribute("projectUrl", projectUrl);
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
		List<String> modifiers = new ArrayList<>();
		{
			for (ClassDoc interfaceDoc : classDoc.interfaces()) {
				ClassDoc superClass = interfaceDoc;
				boolean isAnnotation = false;
				do {
					//the "isAnnotationType" and "isAnnotationTypeElement" methods don't work... o_O
					String name = superClass.qualifiedTypeName();
					if ("java.lang.annotation.Annotation".equals(name)) {
						modifiers.add("annotation");
						isAnnotation = true;
						break;
					}
				} while ((superClass = superClass.superclass()) != null);

				if (isAnnotation) {
					break;
				}
			}

			if (classDoc.isException()) {
				modifiers.add("exception");
			} else if (classDoc.isEnum()) {
				modifiers.add("enum");
			} else if (classDoc.isClass()) {
				modifiers.add("class");
			}
			//note: "interface" is already included in the "modifiers()" method for interfaces

			for (String modifier : classDoc.modifiers().split("\\s+")) {
				modifiers.add(modifier);
			}

			if (modifiers.contains("annotation")) {
				modifiers.remove("interface");
			}
		}
		if (!modifiers.isEmpty()) {
			element.setAttribute("modifiers", String.join(" ", modifiers));
		}

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
		Set<String> addedMethods = new HashSet<>();
		Element methodsElement = document.createElement("methods");
		for (MethodDoc method : classDoc.methods()) {
			String signature = getMethodSignature(method);
			addedMethods.add(signature);

			methodsElement.appendChild(parseMethod(method, document));
		}
		element.appendChild(methodsElement);

		//inherited methods
		//		superClass = classDoc;
		//		while ((superClass = superClass.superclass()) != null) {
		//			for (MethodDoc method : superClass.methods()) {
		//				String signature = getMethodSignature(method);
		//				if (addedMethods.contains(signature)) {
		//					continue;
		//				}
		//				addedMethods.add(signature);
		//
		//				methodsElement.appendChild(parseMethod(method, document));
		//			}
		//		}

		//TODO java.lang.Object methods

		return element;
	}

	private static String getMethodSignature(MethodDoc method) {
		StringBuilder sb = new StringBuilder();
		sb.append(method.name()).append('(');
		boolean first = true;
		for (Parameter parameter : method.parameters()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(parameter.type().simpleTypeName()).append(parameter.type().dimension()).append(' ').append(parameter.name());
		}
		sb.append(')');
		return sb.toString();
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
				sb.append("<code>").append(text).append("</code>");
				break;
			case "@link":
			case "@linkplain":
				//TODO format as a <a> link
				String split[] = text.split("\\s+", 2);
				sb.append(split[split.length - 1]);
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
		if (prettyPrint) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		}

		DOMSource source = new DOMSource(node);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
	}

	private static class StatusPrinter {
		private final int numClasses;
		private int curClass = 1;
		private int prevMessageLength = 0;

		public StatusPrinter(int numClasses) {
			this.numClasses = numClasses;
		}

		public void print(ClassDoc classDoc) {
			StringBuilder sb = new StringBuilder();
			sb.append("Parsing ").append(curClass++).append("/").append(numClasses);
			sb.append(" (").append(classDoc.simpleTypeName()).append(")");

			int curMessageLength = sb.length();
			int spaces = prevMessageLength - curMessageLength;
			for (int i = 0; i < spaces; i++) {
				sb.append(' ');
			}

			System.out.print("\r" + sb.toString());

			prevMessageLength = curMessageLength;
		}
	}
}

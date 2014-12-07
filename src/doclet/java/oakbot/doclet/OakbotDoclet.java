package oakbot.doclet;

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
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * A custom Javadoc doclet that saves class information to XML files in a ZIP
 * file.
 * @author Michael Angstadt
 */
public class OakbotDoclet {
	/**
	 * The entry point for the {@code javadoc} command.
	 * @param rootDoc the root
	 * @return true if successful, false if not
	 * @throws Exception if an error occurred during the parsing
	 */
	public static boolean start(RootDoc rootDoc) throws Exception {
		System.out.println("Building ZIP file...");

		Path zip = Paths.get("java8.zip");

		URI uri = URI.create("jar:file:" + zip.toAbsolutePath());
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
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

	private static Element parseClass(ClassDoc classDoc, Document document) {
		Element element = document.createElement("class");

		String fullName = classDoc.qualifiedTypeName();
		element.setAttribute("fullName", fullName);

		String simpleName = classDoc.simpleTypeName();
		element.setAttribute("simpleName", simpleName);

		String modifiers = classDoc.modifiers();
		element.setAttribute("modifiers", modifiers);

		ClassDoc superClass = classDoc.superclass();
		if (superClass != null) {
			element.setAttribute("extends", superClass.qualifiedTypeName());
		}

		List<String> implementsList = new ArrayList<>();
		for (ClassDoc interfaceDoc : classDoc.interfaces()) {
			implementsList.add(interfaceDoc.qualifiedTypeName());
		}
		if (!implementsList.isEmpty()) {
			element.setAttribute("implements", String.join(" ", implementsList));
		}

		for (AnnotationDesc annotation : classDoc.annotations()) {
			if ("Deprecated".equals(annotation.annotationType().simpleTypeName())) {
				element.setAttribute("deprecated", "true");
				break;
			}
		}

		String description = toMarkdown(classDoc.inlineTags());
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		Element constructorsElement = document.createElement("constructors");
		for (ConstructorDoc constructor : classDoc.constructors()) {
			constructorsElement.appendChild(parseConstructor(constructor, document));
		}
		element.appendChild(constructorsElement);

		Element methodsElement = document.createElement("methods");
		for (MethodDoc method : classDoc.methods()) {
			methodsElement.appendChild(parseMethod(method, document));
		}
		element.appendChild(methodsElement);

		return element;
	}

	private static Element parseConstructor(ConstructorDoc constructor, Document document) {
		Element element = document.createElement("constructor");

		for (AnnotationDesc annotation : constructor.annotations()) {
			if ("Deprecated".equals(annotation.annotationType().simpleTypeName())) {
				element.setAttribute("deprecated", "true");
				break;
			}
		}

		List<String> exceptions = new ArrayList<>();
		for (Type type : constructor.thrownExceptionTypes()) {
			exceptions.add(type.qualifiedTypeName());
		}
		if (!exceptions.isEmpty()) {
			element.setAttribute("throws", String.join(" ", exceptions));
		}

		String description = toMarkdown(constructor.inlineTags());
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		Element parametersElement = document.createElement("parameters");
		for (Parameter parameter : constructor.parameters()) {
			parametersElement.appendChild(parseParameter(parameter, document));
		}
		element.appendChild(parametersElement);

		return element;
	}

	private static Element parseMethod(MethodDoc method, Document document) {
		Element element = document.createElement("method");

		String name = method.name();
		element.setAttribute("name", name);

		String modifiers = method.modifiers();
		element.setAttribute("modifiers", modifiers);

		for (AnnotationDesc annotation : method.annotations()) {
			if ("Deprecated".equals(annotation.annotationType().simpleTypeName())) {
				element.setAttribute("deprecated", "true");
				break;
			}
		}

		String returns = method.returnType().qualifiedTypeName();
		element.setAttribute("returns", returns);

		List<String> exceptions = new ArrayList<>();
		for (Type type : method.thrownExceptionTypes()) {
			exceptions.add(type.qualifiedTypeName());
		}
		if (!exceptions.isEmpty()) {
			element.setAttribute("throws", String.join(" ", exceptions));
		}

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

		Element parametersElement = document.createElement("parameters");
		for (Parameter parameter : method.parameters()) {
			parametersElement.appendChild(parseParameter(parameter, document));
		}
		element.appendChild(parametersElement);

		return element;
	}

	private static MethodDoc findOverriddenMethod(MethodDoc method) {
		MethodDoc m = method.overriddenMethod();
		if (m != null) {
			return m;
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

	private static Element parseParameter(Parameter parameter, Document document) {
		Element element = document.createElement("parameter");

		String name = parameter.name();
		element.setAttribute("name", name);

		String type = parameter.type().qualifiedTypeName();
		element.setAttribute("type", type + parameter.type().dimension());

		return element;
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
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		DOMSource source = new DOMSource(node);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
	}
}

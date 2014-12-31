package oakbot.doclet;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

/**
 * A custom Javadoc doclet that saves class information to XML files inside of a
 * ZIP file.
 * @author Michael Angstadt
 */
public class OakbotDoclet {
	/**
	 * The entry point for the {@code javadoc} command.
	 * @param rootDoc contains the parsed javadoc information
	 * @return true if successful, false if not
	 * @throws Exception if an error occurred during the parsing
	 */
	public static boolean start(RootDoc rootDoc) throws Exception {
		ConfigProperties properties = new ConfigProperties(System.getProperties());

		RootDocXmlProcessor.Builder builder = new RootDocXmlProcessor.Builder();

		builder.libraryName(properties.getLibraryName());

		builder.libraryVersion(properties.getLibraryVersion());

		builder.projectUrl(properties.getProjectUrl());

		builder.baseJavadocUrl(properties.getLibraryBaseUrl());

		boolean prettyPrint = properties.isPrettyPrint();

		Path path = properties.getOutputPath();
		Path outputPath = (path == null) ? Paths.get("javadocs.zip") : path;
		if (Files.exists(outputPath)) {
			Files.delete(outputPath);
		}

		URI uri = URI.create("jar:file:" + outputPath.toAbsolutePath());
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");

		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
			builder.listener(new ListenerImpl(fs, prettyPrint));

			RootDocXmlProcessor parser = builder.build();

			System.out.println("OakBot Doclet");
			System.out.println("Saving to: " + outputPath);

			parser.process(rootDoc);
		}

		return true;
	}

	private static class ListenerImpl implements RootDocXmlProcessor.Listener {
		private final FileSystem fs;
		private final boolean prettyPrint;

		private int curClass = 1;
		private int prevMessageLength = 0;

		public ListenerImpl(FileSystem fs, boolean prettyPrint) {
			this.fs = fs;
			this.prettyPrint = prettyPrint;
		}

		@Override
		public void parsingClass(ClassDoc classDoc, int numClasses) {
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

		@Override
		public void infoCreated(Document document) {
			Path path = fs.getPath("info.xml");
			writeDocument(document, path);
		}

		@Override
		public void classParsed(ClassDoc classDoc, Document document) {
			Path path = fs.getPath(classDoc.qualifiedTypeName() + ".xml");
			writeDocument(document, path);
		}

		private void writeDocument(Node node, Path path) {
			try (Writer writer = Files.newBufferedWriter(path)) {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				if (prettyPrint) {
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				}

				DOMSource source = new DOMSource(node);
				StreamResult result = new StreamResult(writer);
				transformer.transform(source, result);
			} catch (IOException | TransformerException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

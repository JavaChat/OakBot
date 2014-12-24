package oakbot.doclet;

import java.nio.file.Path;
import java.nio.file.Paths;

import oakbot.doclet.RootDocParser.ParseStatusListener;

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
		RootDocParser.Builder builder = new RootDocParser.Builder();
		ConfigProperties properties = new ConfigProperties(System.getProperties());

		Path outputPath = properties.getOutputPath();
		builder.outputPath((outputPath == null) ? Paths.get("javadocs.zip") : outputPath);

		builder.prettyPrint(properties.isPrettyPrint());

		builder.libraryName(properties.getLibraryName());

		builder.libraryVersion(properties.getLibraryVersion());

		builder.projectUrl(properties.getProjectUrl());

		builder.baseJavadocUrl(properties.getLibraryBaseUrl());

		builder.parseStatusListener(new ParseStatusListener() {
			private int curClass = 1;
			private int prevMessageLength = 0;

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
		});

		RootDocParser parser = builder.build();

		System.out.println("OakBot Doclet");
		System.out.println("Saving to: " + outputPath);

		parser.parse(rootDoc);

		return true;
	}
}

package oakbot.doclet.cli;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A command-line interface for using the OakBotDoclet.
 * @author Michael Angstadt
 */
public class JavadocZipFromFile {
	private static final Console console = System.console();

	public static void main(String args[]) throws Exception {
		console.printf("Welcome to the OakBot Javadoc Generator.\n");

		Path sourceJar = getSourceJar();
		String javadocExe = getJavadocExe();
		String docletClasspath = getClasspath();
		String libraryName = getLibraryName();
		String libraryVersion = getLibraryVersion();
		String libraryJavadocUrl = getLibraryJavadocUrl();
		String libraryWebsite = getLibraryWebsite();
		boolean prettyPrint = getPrettyPrint();

		//confirm settings
		console.printf("=============\n");
		console.printf("Source JAR: " + sourceJar + "\n");
		console.printf("Javadoc command: " + javadocExe + "\n");
		console.printf("Library name: " + libraryName + "\n");
		console.printf("Library version: " + libraryVersion + "\n");
		console.printf("Library's base javadoc URL: " + libraryJavadocUrl + "\n");
		console.printf("Library website: " + libraryWebsite + "\n");
		console.printf("Pretty print XML: " + prettyPrint + "\n");
		String answer = console.readLine("Proceed? [Y/n] ");
		if (!answer.isEmpty() && !"y".equalsIgnoreCase(answer)) {
			return;
		}

		console.printf("Extracting files from source JAR...");
		Path sourceDir = unzipSource(sourceJar);
		console.printf("done.\n");
		try {
			//build command
			List<String> commands = new ArrayList<>();
			{
				commands.add(javadocExe);

				commands.add("-doclet");
				commands.add("oakbot.doclet.OakbotDoclet");

				commands.add("-docletpath");
				commands.add(docletClasspath);

				commands.add("-sourcepath");
				commands.add(sourceDir.toString());

				for (String subpackage : getSubpackages(sourceDir)) {
					commands.add("-subpackages");
					commands.add(subpackage);
				}

				commands.add("-quiet");

				commands.add("-J-Doakbot.doclet.output.path=" + libraryName + "-" + libraryVersion + ".zip");
				commands.add("-J-Doakbot.doclet.output.prettyPrint=" + prettyPrint);
				commands.add("-J-Doakbot.doclet.library.name=" + libraryName);
				commands.add("-J-Doakbot.doclet.library.version=" + libraryVersion);
				commands.add("-J-Doakbot.doclet.library.baseUrl=" + libraryJavadocUrl);
				commands.add("-J-Doakbot.doclet.library.projectUrl=" + libraryWebsite);
				commands.add("-J-Xmx1024m");
			}

			//run command
			console.printf("Starting doclet...\n");
			ProcessBuilder builder = new ProcessBuilder(commands);
			Process process = builder.start();
			InputStream in = process.getInputStream();
			int read;
			while ((read = in.read()) != -1) {
				System.out.print((char) read);
			}
			process.waitFor();
		} finally {
			console.printf("Cleaning up...");
			rmdirRecursive(sourceDir);
			console.printf("done.\n");
		}
	}

	private static Path getSourceJar() {
		String answer = console.readLine("Source code JAR: ");
		return Paths.get(answer); //TODO auto-download dependencies
	}

	private static String getJavadocExe() {
		String javaHomeEnv = System.getenv("JAVA_HOME");
		String defaultValue = (javaHomeEnv == null) ? "" : " [" + javaHomeEnv + "]";

		String answer = console.readLine("Java Home" + defaultValue + ": ");
		if (!answer.isEmpty()) {
			return answer;
		}

		if (javaHomeEnv != null) {
			return Paths.get(javaHomeEnv, "bin", "javadoc").toString();
		}

		return "javadoc";
	}

	private static String getClasspath() {
		URL urls[] = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
		List<String> paths = new ArrayList<>(urls.length);
		for (URL url : urls) {
			Path path = Paths.get(url.getPath());
			paths.add(path.toString());
		}

		String separator = System.getProperty("path.separator");
		return String.join(separator, paths);
	}

	private static String getLibraryName() {
		return console.readLine("Library name (all lower-case, cannot contain spaces, e.g \"guava\") ");
	}

	private static String getLibraryVersion() {
		return console.readLine("Library version (e.g \"1.0\") ");
	}

	private static String getLibraryJavadocUrl() {
		return console.readLine("Library's base javadoc URL (e.g \"http://jsoup.org/apidocs/\") ");
	}

	private static String getLibraryWebsite() {
		return console.readLine("Library's website (e.g \"http://jsoup.org\") ");
	}

	private static Path unzipSource(Path sourceJar) throws IOException {
		Path dir = Files.createTempDirectory("oakbot.doclet");
		unzip(dir, sourceJar);
		return dir;
	}

	private static List<String> getSubpackages(Path sourceDir) throws IOException {
		List<String> subpackages = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, new Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				if (!Files.isDirectory(entry)) {
					return false;
				}

				String filename = entry.getFileName().toString();
				return !filename.equals("META-INF");
			}
		})) {
			for (Path path : stream) {
				subpackages.add(path.getFileName().toString());
			}
		}

		return subpackages;
	}

	private static boolean getPrettyPrint() {
		String answer = console.readLine("Pretty-print the XML? [y/N] ");
		return "y".equalsIgnoreCase(answer);
	}

	/**
	 * Extracts all the files in a ZIP file.
	 * @param destinationDir the destination directory.
	 * @param zipFile the ZIP file
	 * @throws IOException
	 */
	private static void unzip(Path destinationDir, Path zipFile) throws IOException {
		try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				String zipPath = entry.getName();
				Path destFile = destinationDir.resolve(zipPath);

				//entry is a directory
				if (zipPath.endsWith("/")) {
					if (!Files.exists(destFile)) {
						Files.createDirectories(destFile);
					}
					continue;
				}

				//make sure the parent directory exists
				Path parent = destFile.getParent();
				if (!Files.exists(parent)) {
					Files.createDirectories(parent);
				}

				//copy the file
				Files.copy(zin, destFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	//http://stackoverflow.com/a/8685959/13379
	private static void rmdirRecursive(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				// try to delete the file anyway, even if its attributes
				// could not be read, since delete-only access is
				// theoretically possible
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed; propagate exception
					throw exc;
				}
			}
		});
	}
}

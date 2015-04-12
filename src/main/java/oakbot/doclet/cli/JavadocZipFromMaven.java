package oakbot.doclet.cli;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import oakbot.doclet.OakbotDoclet;

/**
 * A command-line interface for generating a Javadoc ZIP file for OakBot. The
 * library source code and its dependencies are automatically downloaded from
 * Maven Central.
 * @author Michael Angstadt
 */
public class JavadocZipFromMaven {
	private static final Console console = System.console();
	private static Path tempDir;
	private static boolean verbose = false;

	public static void main(String args[]) throws Exception {
		List<String> argsList = Arrays.asList(args);
		verbose = argsList.contains("-v") || argsList.contains("--verbose");

		String javadocExe = getJavadocExe();
		if (javadocExe == null) {
			return;
		}

		console.printf("Welcome to the OakBot Javadoc Generator.%n");

		//get input from user
		MavenLibrary library = getLibrary();
		String libraryName = getLibraryName(library.artifactId);
		String libraryVersion = getLibraryVersion(library.version);
		String libraryJavadocUrl = getLibraryJavadocUrl();
		String libraryWebsite = getLibraryWebsite();
		boolean prettyPrint = getPrettyPrint();

		//confirm settings
		console.printf("=============Confirmation=============%n");
		console.printf("Javadoc executable: " + javadocExe + "%n");
		console.printf("Maven ID: " + library + "%n");
		console.printf("Library name: " + libraryName + "%n");
		console.printf("Library version: " + libraryVersion + "%n");
		console.printf("Library's base javadoc URL: " + libraryJavadocUrl + "%n");
		console.printf("Library website: " + libraryWebsite + "%n");
		console.printf("Pretty print XML: " + prettyPrint + "%n");
		String answer = console.readLine("Proceed? [Y/n] ");
		if (!answer.isEmpty() && !"y".equalsIgnoreCase(answer)) {
			return;
		}

		tempDir = Files.createTempDirectory("oakbot.doclet");

		try {
			Path sourceJar = downloadSource(library);
			downloadPom(library);
			List<Path> dependencyJars = downloadDependencies();
			Path sourceDir = unzipSource(sourceJar);

			List<String> commands = buildJavadocArgs(javadocExe, dependencyJars, sourceDir, libraryName, libraryVersion, libraryJavadocUrl, libraryWebsite, prettyPrint);
			runJavadoc(commands);
		} finally {
			deleteTempDir();
		}
	}

	private static void deleteTempDir() throws IOException {
		console.printf("Cleaning up...");
		rmdirRecursive(tempDir);
		console.printf("done.%n");
	}

	private static List<String> buildJavadocArgs(String javadocExe, List<Path> dependencies, Path source, String name, String version, String javadocUrl, String website, boolean prettyPrint) throws IOException {
		List<String> commands = new ArrayList<>();
		commands.add(javadocExe);

		commands.add("-doclet");
		commands.add(OakbotDoclet.class.getName());

		String docletClasspath = getClasspath();
		commands.add("-docletpath");
		commands.add(docletClasspath);

		if (!dependencies.isEmpty()) {
			commands.add("-classpath");
			commands.add(buildClasspath(dependencies));
		}

		commands.add("-sourcepath");
		commands.add(source.toString());

		for (String subpackage : getSubpackages(source)) {
			commands.add("-subpackages");
			commands.add(subpackage);
		}

		commands.add("-quiet");

		commands.add("-J-Doakbot.doclet.output.path=" + name + "-" + version + ".zip");
		commands.add("-J-Doakbot.doclet.output.prettyPrint=" + prettyPrint);
		commands.add("-J-Doakbot.doclet.library.name=" + name);
		commands.add("-J-Doakbot.doclet.library.version=" + version);
		if (!javadocUrl.isEmpty()) {
			commands.add("-J-Doakbot.doclet.library.baseUrl=" + javadocUrl);
		}
		if (!website.isEmpty()) {
			commands.add("-J-Doakbot.doclet.library.projectUrl=" + website);
		}
		commands.add("-J-Xmx1024m");

		return commands;
	}

	private static void runJavadoc(List<String> commands) throws IOException, InterruptedException {
		if (verbose) {
			console.printf("Starting doclet with commands: " + commands + "%n");
		} else {
			console.printf("Starting doclet...%n");
		}

		ProcessBuilder builder = new ProcessBuilder(commands);
		Process process = builder.start();
		InputStream in = process.getInputStream();
		int read;
		while ((read = in.read()) != -1) {
			System.out.print((char) read);
		}
		process.waitFor();
	}

	private static List<Path> downloadDependencies() throws IOException, InterruptedException {
		//use Maven to perform the dependency resolution
		ProcessBuilder builder = new ProcessBuilder("mvn", "dependency:copy-dependencies");
		builder.directory(tempDir.toFile());
		Process process = builder.start();
		InputStream in = process.getInputStream();
		int read;
		while ((read = in.read()) != -1) {
			System.out.print((char) read);
		}
		process.waitFor();

		//build a list of all the JARs
		List<Path> jars = new ArrayList<>();
		Path dir = tempDir.resolve(Paths.get("target", "dependency"));
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, new Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				String filename = entry.getFileName().toString();
				return filename.endsWith(".jar");
			}
		})) {
			for (Path path : stream) {
				jars.add(path);
			}
		}
		return jars;
	}

	private static Path downloadPom(MavenLibrary library) throws IOException {
		console.printf("Downloading project POM...");
		Path dest = tempDir.resolve("pom.xml");
		String url = library.getPomUrl();
		download(url, dest, null);
		console.printf("done.%n");
		return dest;
	}

	private static Path downloadSource(MavenLibrary library) throws IOException {
		String url = library.getSourcesUrl();
		int pos = url.lastIndexOf('/');
		String filename = url.substring(pos + 1);
		Path dest = tempDir.resolve(filename);

		download(url, dest, new Progress() {
			@Override
			public void progress(int downloaded, int size) {
				int percent = (int) ((double) downloaded / size * 100);
				console.printf("\rDownloading " + filename + " (" + (downloaded / 1024) + "KB / " + (size / 1024) + "KB, " + percent + "%%)");
			}
		});
		console.printf("%n");

		return dest;
	}

	private static MavenLibrary getLibrary() {
		String answer = console.readLine("Maven ID: (e.g. \"com.googlecode.ez-vcard:ez-vcard:0.9.6\"): ").trim();
		return MavenLibrary.parse(answer);
	}

	private static String getJavadocExe() {
		String javaHomeEnv = System.getenv("JAVA_HOME");
		if (javaHomeEnv == null) {
			console.printf("Please set your JAVA_HOME environment variable:%nJAVA_HOME=/path/to/jdk%nexport JAVA_HOME%n");
			return null;
		}

		return Paths.get(javaHomeEnv, "bin", "javadoc").toString();
	}

	private static String getLibraryName(String defaultValue) {
		String answer = console.readLine("Library name [" + defaultValue + "]: ").trim();
		return answer.isEmpty() ? defaultValue : answer;
	}

	private static String getLibraryVersion(String defaultValue) {
		String answer = console.readLine("Library version [" + defaultValue + "]: ").trim();
		return answer.isEmpty() ? defaultValue : answer;
	}

	private static String getLibraryJavadocUrl() {
		return console.readLine("Library's base javadoc URL (optional): ").trim();
	}

	private static String getLibraryWebsite() {
		return console.readLine("Library's website (optional): ").trim();
	}

	private static boolean getPrettyPrint() {
		String answer = console.readLine("Pretty-print the XML? [y/N] ");
		return "y".equalsIgnoreCase(answer);
	}

	private static Path unzipSource(Path sourceJar) throws IOException {
		console.printf("Extracting files from source JAR...");
		Path dir = tempDir.resolve("src");
		Files.createDirectory(dir);
		unzip(dir, sourceJar);
		console.printf("done.%n");
		return dir;
	}

	private static String getClasspath() {
		URL urls[] = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
		List<Path> paths = new ArrayList<>(urls.length);
		for (URL url : urls) {
			Path path = Paths.get(url.getPath());
			paths.add(path);
		}

		return buildClasspath(paths);
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

	/**
	 * Builds a classpath string.
	 * @param paths the files and directories to include in the classpath
	 * @return the classpath string
	 */
	private static String buildClasspath(Collection<Path> paths) {
		List<String> pathStrings = new ArrayList<>(paths.size());
		for (Path path : paths) {
			pathStrings.add(path.toString());
		}

		String separator = System.getProperty("path.separator");
		return String.join(separator, pathStrings);
	}

	/**
	 * Downloads a file from the Internet.
	 * @param url the file URL
	 * @param destination where to save the file to
	 * @param progress callback for monitoring the download progress (can be
	 * null)
	 * @throws IOException
	 */
	private static void download(String url, Path destination, Progress progress) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		try (InputStream in = connection.getInputStream()) {
			if (progress == null) {
				Files.copy(in, destination);
				return;
			}

			int size = connection.getContentLength();
			int downloaded = 0;
			byte buffer[] = new byte[4092];
			try (OutputStream out = Files.newOutputStream(destination)) {
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
					downloaded += read;
					progress.progress(downloaded, size);
				}
			}
		}
	}

	private interface Progress {
		void progress(int downloaded, int size);
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
				Files.copy(zin, destFile);
			}
		}
	}

	/**
	 * Recursively deletes a directory.
	 * @param directory the directory to delete
	 * @throws IOException
	 * @see http://stackoverflow.com/a/8685959/13379
	 */
	private static void rmdirRecursive(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
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

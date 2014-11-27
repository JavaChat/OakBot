package oakbot.javadoc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads Javadoc pages from a ZIP file. The root the ZIP file must contain the
 * "allclasses-frame.html" file.
 * @author Michael Angstadt
 */
public class ZipPageLoader implements PageLoader {
	private static final String allClassesFrameFileName = "allclasses-frame.html";
	private final Path file;

	/**
	 * @param file the path to the ZIP file.
	 * @throws IOException if there's a problem reading the ZIP file
	 * @throws IllegalArgumentException if there is no "allclasses-frame.html"
	 * file at the root
	 */
	public ZipPageLoader(Path file) throws IOException {
		//make sure it contains the "allclasses-frame.html" file
		try (FileSystem fs = FileSystems.newFileSystem(file, null)) {
			Path allClassesFile = fs.getPath("/" + allClassesFrameFileName);
			if (!Files.exists(allClassesFile)) {
				throw new IllegalArgumentException("\"" + allClassesFrameFileName + "\" not found in ZIP root.");
			}
		}

		this.file = file;
	}

	@Override
	public InputStream getClassPage(String className) throws IOException {
		Path htmlFile = Paths.get("/" + className.replace('.', '/') + ".html");
		FileSystem fs = FileSystems.newFileSystem(file, null);
		Path path = fs.getPath(htmlFile.toString());
		if (Files.exists(path)) {
			return new ZipFileInputStream(fs, path);
		}

		fs.close();
		return null;
	}

	@Override
	public InputStream getAllClassesFile() throws IOException {
		FileSystem fs = FileSystems.newFileSystem(file, null);
		Path allClassesFile = fs.getPath("/" + allClassesFrameFileName);
		return new ZipFileInputStream(fs, allClassesFile);
	}

	private static class ZipFileInputStream extends InputStream {
		private final FileSystem fs;
		private final InputStream in;

		public ZipFileInputStream(FileSystem fs, Path path) throws IOException {
			this.fs = fs;
			this.in = Files.newInputStream(path);
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}

		@Override
		public void close() throws IOException {
			try {
				in.close();
			} catch (IOException e) {
				//ignore
			}
			fs.close();
		}
	}
}

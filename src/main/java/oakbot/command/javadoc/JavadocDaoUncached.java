package oakbot.command.javadoc;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Stores nothing in memory, allowing for lower memory usage.
 * @author Michael Angstadt
 */
public class JavadocDaoUncached implements JavadocDao {
	private final Path dir;

	/**
	 * @param dir the directory where the Javadoc ZIP files are stored
	 */
	public JavadocDaoUncached(Path dir) {
		this.dir = dir;
	}

	@Override
	public Collection<String> search(String query) throws IOException {
		var names = new HashSet<String>();
		for (var file : getZipFiles()) {
			var zip = new JavadocZipFile(file);
			for (var className : zip.getClassNames()) {
				var fullName = className.getFullyQualifiedName();
				var simpleName = className.getSimpleName();
				if (fullName.equalsIgnoreCase(query) || simpleName.equalsIgnoreCase(query)) {
					names.add(fullName);
				}
			}
		}
		return names;
	}

	@Override
	public ClassInfo getClassInfo(String fullyQualifiedClassName) throws IOException {
		for (var file : getZipFiles()) {
			var zip = new JavadocZipFile(file);
			var info = zip.getClassInfo(fullyQualifiedClassName);
			if (info != null) {
				return info;
			}
		}

		return null;
	}

	private List<Path> getZipFiles() throws IOException {
		var files = new ArrayList<Path>();
		try (var stream = Files.newDirectoryStream(dir, JavadocDaoUncached::isZipFile)) {
			stream.forEach(files::add);
		}
		return files;
	}

	/**
	 * Determines if a file has a ".zip" extension (case sensitive).
	 * @param file the file
	 * @return true if it does, false it not
	 */
	private static boolean isZipFile(Path file) {
		return file.getFileName().toString().toLowerCase().endsWith(".zip");
	}
}

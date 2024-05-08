package oakbot.command.javadoc;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Stores metadata in memory to allow for faster lookup times.
 * @author Michael Angstadt
 */
public class JavadocDaoCached implements JavadocDao {
	private static final Logger logger = Logger.getLogger(JavadocDaoCached.class.getName());

	/**
	 * Maps each Javadoc ZIP file to the list of classes it contains.
	 * 
	 * <ul>
	 * <li><b>Key:</b> The ZIP file.</li>
	 * <li><b>Value:</b> The fully-qualified class names of the classes
	 * contained within the ZIP file.</li>
	 * </ul>
	 */
	private final Multimap<JavadocZipFile, String> libraryClasses = HashMultimap.create();

	/**
	 * Maps class name aliases to their fully qualified names. For example, maps
	 * "string" to "java.lang.String". Note that there can be more than one
	 * class name mapped to an alias (for example "list" is mapped to
	 * "java.util.List" and "java.awt.List").
	 * 
	 * <ul>
	 * <li><b>Key:</b> The alias (in lower case)</li>
	 * <li><b>Value:</b> The fully-qualified class names that are mapped to the
	 * alias.</li>
	 * </ul>
	 */
	private final Multimap<String, String> aliases = HashMultimap.create();

	/**
	 * Caches class info that was parsed from the Javadoc ZIP files.
	 * 
	 * <ul>
	 * <li><b>Key:</b> The fully-qualified class name.</li>
	 * <li><b>Value:</b> The parsed Javadoc info for the class.</li>
	 * </ul>
	 */
	private final Map<String, ClassInfo> cache = new HashMap<>();

	/**
	 * @param dir the directory where the Javadoc ZIP files are stored
	 * @throws IOException if there's a problem reading any of the ZIP files
	 */
	public JavadocDaoCached(Path dir) throws IOException {
		try (var stream = Files.newDirectoryStream(dir, JavadocDaoCached::isZipFile)) {
			for (var file : stream) {
				register(file);
			}
		}

		var watchThread = new WatchThread(dir);
		watchThread.start();
	}

	/**
	 * Registers a Javadoc ZIP file with the DAO.
	 * @param file the ZIP file containing the Javadoc info (generated by
	 * oakbot-doclet)
	 * @throws IOException if there was a problem reading the ZIP file
	 */
	private void register(Path file) throws IOException {
		var zip = new JavadocZipFile(file);
		for (var className : zip.getClassNames()) {
			var fullName = className.getFullyQualifiedName();
			var simpleName = className.getSimpleName();

			aliases.put(simpleName.toLowerCase(), fullName);
			aliases.put(simpleName, fullName);
			aliases.put(fullName.toLowerCase(), fullName);
			aliases.put(fullName, fullName);
			libraryClasses.put(zip, fullName);
		}
	}

	@Override
	public synchronized Collection<String> search(String className) {
		var names = aliases.get(className);
		if (names.isEmpty()) {
			//try case-insensitive search
			names = aliases.get(className.toLowerCase());
		}
		return names;
	}

	@Override
	public synchronized ClassInfo getClassInfo(String fullyQualifiedClassName) throws IOException {
		//check the cache
		var info = cache.get(fullyQualifiedClassName);
		if (info != null) {
			return info;
		}

		//parse the class info from the Javadocs
		for (var zip : libraryClasses.keySet()) {
			info = zip.getClassInfo(fullyQualifiedClassName);
			if (info != null) {
				cache.put(fullyQualifiedClassName, info);
				return info;
			}
		}

		return null;
	}

	/**
	 * Watches the Javadoc directory for changes, loading or removing Javadoc
	 * ZIP files that were added or deleted from the file system.
	 */
	private class WatchThread extends Thread {
		private final Path dir;
		private final WatchService watcher;

		/**
		 * @param dir the directory to watch
		 * @throws IOException if there's a problem watching the directory
		 */
		public WatchThread(Path dir) throws IOException {
			setName(getClass().getSimpleName());
			setDaemon(true);

			watcher = FileSystems.getDefault().newWatchService();
			dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			this.dir = dir;
		}

		@Override
		public void run() {
			while (true) {
				WatchKey key;
				try {
					key = watcher.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.log(Level.WARNING, e, () -> "Thread interrupted while watching for changes to the Javadoc ZIP files.");
					break;
				}

				key.pollEvents().forEach(this::handleEvent);

				var valid = key.reset();
				if (!valid) {
					logger.warning(() -> "Javadoc ZIP file watch thread has been terminated due to the watch key becoming invalid.");
					break;
				}
			}
		}

		private void handleEvent(WatchEvent<?> event) {
			var kind = event.kind();
			if (kind == StandardWatchEventKinds.OVERFLOW) {
				return;
			}

			@SuppressWarnings("unchecked")
			var file = ((WatchEvent<Path>) event).context();
			if (!isZipFile(file)) {
				return;
			}

			file = dir.resolve(file);

			if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
				add(file);
				return;
			}

			if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
				remove(file);
				return;
			}

			if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
				remove(file);
				add(file);
				return;
			}
		}

		private void add(Path file) {
			logger.info(() -> "Loading ZIP file " + file + "...");
			try {
				register(file);
				logger.info(() -> "ZIP file " + file + " loaded.");
			} catch (Exception e) {
				//catch RuntimeExceptions too
				logger.log(Level.SEVERE, e, () -> "Could not parse Javadoc ZIP file.  ZIP file was not added to the JavadocDao.");
			}
		}

		private void remove(Path file) {
			logger.info(() -> "Removing ZIP file " + file + "...");
			var fileName = file.getFileName();

			synchronized (JavadocDaoCached.this) {
				//find the corresponding JavadocZipFile object
				//@formatter:off
				var found = libraryClasses.keys().stream()
					.filter(zip -> zip.getPath().getFileName().equals(fileName))
				.findAny();
				//@formatter:on

				if (found.isEmpty()) {
					logger.warning(() -> "Tried to remove ZIP file \"" + file + "\", but it was not found in the JavadocDao.");
					return;
				}

				var classNames = libraryClasses.removeAll(found.get());
				aliases.values().removeAll(classNames);
				cache.keySet().removeAll(classNames);
			}

			logger.info(() -> "ZIP file " + file + " removed.");
		}
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

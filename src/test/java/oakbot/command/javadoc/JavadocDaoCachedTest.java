package oakbot.command.javadoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Michael Angstadt
 */
class JavadocDaoCachedTest {
	@TempDir
	private Path tempDir;

	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDaoCached dao;
	{
		try {
			dao = new JavadocDaoCached(root);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void search_multiple_results() {
		var actual = new HashSet<>(dao.search("list"));
		var expected = new HashSet<>(List.of("java.awt.List", "java.util.List"));
		assertEquals(expected, actual);
	}

	@Test
	void search_single_result() {
		var actual = new HashSet<>(dao.search("java.awt.list"));
		var expected = new HashSet<>(List.of("java.awt.List"));
		assertEquals(expected, actual);
	}

	@Test
	void search_no_results() {
		var names = dao.search("lsit");
		assertTrue(names.isEmpty());
	}

	@Test
	void getClassInfo() throws Exception {
		var info = dao.getClassInfo("java.util.List");
		assertEquals("java.util.List", info.getName().getFullyQualifiedName());
	}

	@Test
	void getClassInfo_case_sensitive() throws Exception {
		var info = dao.getClassInfo("java.util.list");
		assertNull(info);
	}

	@Test
	void directory_watcher_ignore_non_zip_files() throws Exception {
		var dao = new JavadocDaoCached(tempDir);

		assertNull(dao.getClassInfo("java.util.List"));

		var source = root.resolve("JavadocZipFileTest.zip");
		var dest = tempDir.resolve("JavadocZipFileTest.txt");
		Files.copy(source, dest);
		Thread.sleep(1000);
		assertNull(dao.getClassInfo("java.util.List"));
	}

	@Test
	void directory_watcher_add() throws Exception {
		var dao = new JavadocDaoCached(tempDir);

		assertNull(dao.getClassInfo("java.util.List"));

		var source = root.resolve("JavadocZipFileTest.zip");
		var dest = tempDir.resolve("JavadocZipFileTest.zip");
		Files.copy(source, dest);

		//wait for the WatchService to pick up the file
		//this is really slow on Macs, see: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
		var start = System.currentTimeMillis();
		ClassInfo info = null;
		while (info == null && (System.currentTimeMillis() - start) < TimeUnit.SECONDS.toMillis(5)) {
			Thread.sleep(200);
			info = dao.getClassInfo("java.util.List");
		}
		assertNotNull(info);
	}

	@Test
	void directory_watcher_remove() throws Exception {
		var source = root.resolve("JavadocZipFileTest.zip");
		var dest = tempDir.resolve("JavadocZipFileTest.zip");
		Files.copy(source, dest);

		var dao = new JavadocDaoCached(tempDir);

		var info = dao.getClassInfo("java.util.List");
		assertNotNull(info);

		source = tempDir.resolve("JavadocZipFileTest.zip");
		Files.delete(source);

		//wait for the WatchService to pick up the deleted file
		//this is really slow on Macs, see: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
		var start = System.currentTimeMillis();
		while (info != null && (System.currentTimeMillis() - start) < TimeUnit.SECONDS.toMillis(5)) {
			Thread.sleep(200);
			info = dao.getClassInfo("java.util.List");
		}
		assertNull(info);
	}

	@Test
	void directory_watcher_modified() throws Exception {
		var source = root.resolve("JavadocZipFileTest.zip");
		var dest = tempDir.resolve("JavadocZipFileTest.zip");
		Files.copy(source, dest);

		Thread.sleep(1500); //wait a bit before modifying the file so the timestamp is significantly different (for Macs)

		var dao = new JavadocDaoCached(tempDir);

		var info = dao.getClassInfo("java.util.List");
		assertNotNull(info);

		try (var fs = FileSystems.newFileSystem(dest)) {
			var path = fs.getPath("java/util/List.xml");
			Files.delete(path);
		}

		//wait for the WatchService to pick up the change
		//this is really slow on Macs, see: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
		var start = System.currentTimeMillis();
		while (info != null && (System.currentTimeMillis() - start) < TimeUnit.SECONDS.toMillis(5)) {
			Thread.sleep(200);
			info = dao.getClassInfo("java.util.List");
		}
		assertNull(info);
	}
}

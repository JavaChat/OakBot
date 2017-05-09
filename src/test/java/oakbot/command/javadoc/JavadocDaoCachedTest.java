package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Michael Angstadt
 */
public class JavadocDaoCachedTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDaoCached dao;
	{
		try {
			dao = new JavadocDaoCached(root);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void search_multiple_results() {
		Collection<String> names = dao.search("list");
		Set<String> actual = new HashSet<>(names);
		Set<String> expected = new HashSet<>(Arrays.asList("java.awt.List", "java.util.List"));
		assertEquals(expected, actual);
	}

	@Test
	public void search_single_result() {
		Collection<String> names = dao.search("java.awt.list");
		Set<String> actual = new HashSet<>(names);
		Set<String> expected = new HashSet<>(Arrays.asList("java.awt.List"));
		assertEquals(expected, actual);
	}

	@Test
	public void search_no_results() {
		Collection<String> names = dao.search("lsit");
		assertTrue(names.isEmpty());
	}

	@Test
	public void getClassInfo() throws Exception {
		ClassInfo info = dao.getClassInfo("java.util.List");
		assertEquals("java.util.List", info.getName().getFullyQualifiedName());
	}

	@Test
	public void getClassInfo_case_sensitive() throws Exception {
		ClassInfo info = dao.getClassInfo("java.util.list");
		assertNull(info);
	}

	@Test
	public void directory_watcher_ignore_non_zip_files() throws Exception {
		Path dir = temporaryFolder.getRoot().toPath();
		JavadocDaoCached dao = new JavadocDaoCached(dir);

		assertNull(dao.getClassInfo("java.util.List"));

		Path source = root.resolve("JavadocZipFileTest.zip");
		Path dest = dir.resolve("JavadocZipFileTest.txt");
		Files.copy(source, dest);
		Thread.sleep(1000);
		assertNull(dao.getClassInfo("java.util.List"));
	}

	@Test
	public void directory_watcher_add() throws Exception {
		Path dir = temporaryFolder.getRoot().toPath();
		JavadocDaoCached dao = new JavadocDaoCached(dir);

		assertNull(dao.getClassInfo("java.util.List"));

		Path source = root.resolve("JavadocZipFileTest.zip");
		Path dest = dir.resolve("JavadocZipFileTest.zip");
		Files.copy(source, dest);

		//wait for the WatchService to pick up the file
		//this is really slow on Macs, see: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
		long start = System.currentTimeMillis();
		ClassInfo info = null;
		while (info == null && (System.currentTimeMillis() - start) < TimeUnit.SECONDS.toMillis(5)) {
			Thread.sleep(200);
			info = dao.getClassInfo("java.util.List");
		}
		assertNotNull(info);
	}

	@Test
	public void directory_watcher_remove() throws Exception {
		Path dir = temporaryFolder.getRoot().toPath();
		Path source = root.resolve("JavadocZipFileTest.zip");
		Path dest = dir.resolve("JavadocZipFileTest.zip");
		Files.copy(source, dest);

		JavadocDaoCached dao = new JavadocDaoCached(dir);

		ClassInfo info = dao.getClassInfo("java.util.List");
		assertNotNull(info);

		source = dir.resolve("JavadocZipFileTest.zip");
		Files.delete(source);

		//wait for the WatchService to pick up the deleted file
		//this is really slow on Macs, see: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
		long start = System.currentTimeMillis();
		while (info != null && (System.currentTimeMillis() - start) < TimeUnit.SECONDS.toMillis(5)) {
			Thread.sleep(200);
			info = dao.getClassInfo("java.util.List");
		}
		assertNull(info);
	}

	@Test
	public void directory_watcher_modified() throws Exception {
		Path dir = temporaryFolder.getRoot().toPath();
		Path source = root.resolve("JavadocZipFileTest.zip");
		Path dest = dir.resolve("JavadocZipFileTest.zip");
		Files.copy(source, dest);

		Thread.sleep(1500); //wait a bit before modifying the file so the timestamp is significantly different (for Macs)

		JavadocDaoCached dao = new JavadocDaoCached(dir);

		ClassInfo info = dao.getClassInfo("java.util.List");
		assertNotNull(info);

		try (FileSystem fs = FileSystems.newFileSystem(dest, null)) {
			Path path = fs.getPath("java/util/List.xml");
			Files.delete(path);
		}

		//wait for the WatchService to pick up the change
		//this is really slow on Macs, see: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
		long start = System.currentTimeMillis();
		while (info != null && (System.currentTimeMillis() - start) < TimeUnit.SECONDS.toMillis(5)) {
			Thread.sleep(200);
			info = dao.getClassInfo("java.util.List");
		}
		assertNull(info);
	}
}

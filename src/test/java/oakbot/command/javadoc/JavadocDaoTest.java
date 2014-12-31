package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
public class JavadocDaoTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDao dao;
	{
		try {
			dao = new JavadocDao(root);
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
	public void simpleName_single_match() throws Exception {
		ClassInfo info = dao.getClassInfo("Collection");
		assertEquals("java.util.Collection", info.getName().getFull());
	}

	@Test
	public void simpleName_no_match() throws Exception {
		ClassInfo info = dao.getClassInfo("FooBar");
		assertNull(info);
	}

	@Test
	public void simpleName_case_insensitive() throws Exception {
		ClassInfo info = dao.getClassInfo("collection");
		assertEquals("java.util.Collection", info.getName().getFull());
	}

	@Test
	public void simpleName_multiple_matches() throws Exception {
		try {
			dao.getClassInfo("List");
			fail();
		} catch (MultipleClassesFoundException e) {
			Set<String> actual = new HashSet<>(e.getClasses());
			Set<String> expected = new HashSet<>(Arrays.asList("java.awt.List", "java.util.List"));
			assertEquals(expected, actual);
		}
	}

	@Test
	public void fullName() throws Exception {
		ClassInfo info = dao.getClassInfo("java.util.List");
		assertEquals("java.util.List", info.getName().getFull());
	}

	@Test
	public void fullName_case_insensitive() throws Exception {
		ClassInfo info = dao.getClassInfo("java.util.list");
		assertEquals("java.util.List", info.getName().getFull());
	}

	@Test
	public void directory_watcher_ignore_non_zip_files() throws Exception {
		Path dir = temporaryFolder.getRoot().toPath();
		JavadocDao dao = new JavadocDao(dir);

		assertNull(dao.getClassInfo("java.util.List"));

		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.txt");
		Files.copy(source, dest);
		Thread.sleep(1000);
		assertNull(dao.getClassInfo("java.util.List"));
	}

	@Test
	public void directory_watcher_add() throws Exception {
		Path dir = temporaryFolder.getRoot().toPath();
		JavadocDao dao = new JavadocDao(dir);

		assertNull(dao.getClassInfo("java.util.List"));

		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.zip");
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
		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.zip");
		Files.copy(source, dest);

		JavadocDao dao = new JavadocDao(dir);

		ClassInfo info = dao.getClassInfo("java.util.List");
		assertNotNull(info);

		source = dir.resolve("LibraryZipFileTest.zip");
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
		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.zip");
		Files.copy(source, dest);
		
		Thread.sleep(1500); //wait a bit before modifying the file so the timestamp is significantly different (for Macs)

		JavadocDao dao = new JavadocDao(dir);

		ClassInfo info = dao.getClassInfo("java.util.List");
		assertNotNull(info);

		try (FileSystem fs = FileSystems.newFileSystem(dest, null)) {
			Path path = fs.getPath("java.util.List.xml");
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

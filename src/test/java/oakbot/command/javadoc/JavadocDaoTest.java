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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Michael Angstadt
 */
public class JavadocDaoTest {
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDao dao;
	{
		try {
			dao = new JavadocDao(root);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		Path dir = temp.getRoot().toPath();
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
		Path dir = temp.getRoot().toPath();
		JavadocDao dao = new JavadocDao(dir);

		assertNull(dao.getClassInfo("java.util.List"));

		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.zip");
		Files.copy(source, dest);
		Thread.sleep(1000);
		assertNotNull(dao.getClassInfo("java.util.List"));
	}

	@Test
	public void directory_watcher_remove() throws Exception {
		Path dir = temp.getRoot().toPath();
		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.zip");
		Files.copy(source, dest);

		JavadocDao dao = new JavadocDao(dir);

		assertNotNull(dao.getClassInfo("java.util.List"));

		source = dir.resolve("LibraryZipFileTest.zip");
		Files.delete(source);
		Thread.sleep(1000);
		assertNull(dao.getClassInfo("java.util.List"));
	}

	@Test
	public void directory_watcher_modified() throws Exception {
		Path dir = temp.getRoot().toPath();
		Path source = root.resolve("LibraryZipFileTest.zip");
		Path dest = dir.resolve("LibraryZipFileTest.zip");
		Files.copy(source, dest);

		JavadocDao dao = new JavadocDao(dir);

		assertNotNull(dao.getClassInfo("java.util.List"));

		try (FileSystem fs = FileSystems.newFileSystem(dest, null)) {
			Path path = fs.getPath("java.util.List.xml");
			Files.delete(path);
		}
		Thread.sleep(1000);
		assertNull(dao.getClassInfo("java.util.List"));
	}
}

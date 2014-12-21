package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import oakbot.command.javadoc.ClassInfo;
import oakbot.command.javadoc.LibraryZipFile;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class LibraryZipFileTest {
	private final Path root = Paths.get("src", "test", "resources", "oakbot", "javadoc");
	private final LibraryZipFile zip;
	{
		Path file = root.resolve(getClass().getSimpleName() + ".zip");
		try {
			zip = new LibraryZipFile(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void info_without_file() throws Exception {
		Path file = root.resolve(getClass().getSimpleName() + "-no-info.zip");
		LibraryZipFile zip = new LibraryZipFile(file);
		assertNull(zip.getName());
		assertNull(zip.getBaseUrl());
	}

	@Test
	public void info_without_attributes() throws Exception {
		Path file = root.resolve(getClass().getSimpleName() + "-no-attributes.zip");
		LibraryZipFile zip = new LibraryZipFile(file);
		assertNull(zip.getName());
		assertNull(zip.getBaseUrl());
	}

	@Test
	public void info() {
		assertEquals("Java", zip.getName());
		assertEquals("8", zip.getVersion());
		assertEquals("https://docs.oracle.com/javase/8/docs/api/", zip.getBaseUrl());
		assertEquals("http://java.oracle.com", zip.getProjectUrl());
	}

	@Test
	public void getClasses() throws Exception {
		Iterator<String> it = zip.getClasses();
		Set<String> actual = new HashSet<>();
		while (it.hasNext()) {
			actual.add(it.next());
		}

		//@formatter:off
		Set<String> expected = new HashSet<>(Arrays.asList(
			"java.lang.Object",
			"java.awt.List",
			"java.util.List",
			"java.util.Collection"
		));
		//@formatter:on

		assertEquals(expected, actual);
	}

	@Test
	public void getClassInfo_not_found() throws Exception {
		ClassInfo info = zip.getClassInfo("java.lang.Foo");
		assertNull(info);
	}

	@Test
	public void getClassInfo() throws Exception {
		ClassInfo info = zip.getClassInfo("java.lang.Object");
		assertEquals("java.lang.Object", info.getName().getFull());
		assertEquals("Object", info.getName().getSimple());
	}
}

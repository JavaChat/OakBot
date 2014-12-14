package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import oakbot.command.javadoc.ClassInfo;
import oakbot.command.javadoc.JavadocDao;
import oakbot.command.javadoc.LibraryZipFile;
import oakbot.command.javadoc.MultipleClassesFoundException;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocDaoTest {
	private final Path root = Paths.get("src", "test", "resources", "oakbot", "javadoc");
	private final JavadocDao dao = new JavadocDao();
	{
		try {
			dao.addApi(root.resolve(LibraryZipFileTest.class.getSimpleName() + ".zip"));
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
	public void cache() throws Exception {
		Path zipFile = root.resolve(LibraryZipFileTest.class.getSimpleName() + ".zip");
		LibraryZipFile spy = spy(new LibraryZipFile(zipFile));

		JavadocDao dao = new JavadocDao();
		dao.addApi(spy);

		dao.getClassInfo("Collection");
		dao.getClassInfo("collection");
		verify(spy, times(1)).getClassInfo("java.util.Collection");
	}
}

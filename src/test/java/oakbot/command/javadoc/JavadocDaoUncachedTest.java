package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Michael Angstadt
 */
public class JavadocDaoUncachedTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDaoUncached dao;
	{
		dao = new JavadocDaoUncached(root);
	}

	@BeforeClass
	public static void beforeClass() {
		//turn off logging
		LogManager.getLogManager().reset();
	}

	@Test
	public void search_multiple_results() throws Exception {
		Collection<String> names = dao.search("list");
		Set<String> actual = new HashSet<>(names);
		Set<String> expected = new HashSet<>(Arrays.asList("java.awt.List", "java.util.List"));
		assertEquals(expected, actual);
	}

	@Test
	public void search_single_result() throws Exception {
		Collection<String> names = dao.search("java.awt.list");
		Set<String> actual = new HashSet<>(names);
		Set<String> expected = new HashSet<>(Arrays.asList("java.awt.List"));
		assertEquals(expected, actual);
	}

	@Test
	public void search_no_results() throws Exception {
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
}

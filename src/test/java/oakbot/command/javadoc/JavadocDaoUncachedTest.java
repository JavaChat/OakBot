package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocDaoUncachedTest {
	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDaoUncached dao;
	{
		dao = new JavadocDaoUncached(root);
	}

	@Test
	public void search_multiple_results() throws Exception {
		var actual = new HashSet<>(dao.search("list"));
		var expected = new HashSet<>(List.of("java.awt.List", "java.util.List"));
		assertEquals(expected, actual);
	}

	@Test
	public void search_single_result() throws Exception {
		var actual = new HashSet<>(dao.search("java.awt.list"));
		var expected = new HashSet<>(List.of("java.awt.List"));
		assertEquals(expected, actual);
	}

	@Test
	public void search_no_results() throws Exception {
		var names = dao.search("lsit");
		assertTrue(names.isEmpty());
	}

	@Test
	public void getClassInfo() throws Exception {
		var info = dao.getClassInfo("java.util.List");
		assertEquals("java.util.List", info.getName().getFullyQualifiedName());
	}

	@Test
	public void getClassInfo_case_sensitive() throws Exception {
		var info = dao.getClassInfo("java.util.list");
		assertNull(info);
	}
}

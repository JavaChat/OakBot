package oakbot.command.javadoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class JavadocDaoUncachedTest {
	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocDaoUncached dao;
	{
		dao = new JavadocDaoUncached(root);
	}

	@Test
	void search_multiple_results() throws Exception {
		var actual = new HashSet<>(dao.search("list"));
		var expected = new HashSet<>(List.of("java.awt.List", "java.util.List"));
		assertEquals(expected, actual);
	}

	@Test
	void search_single_result() throws Exception {
		var actual = new HashSet<>(dao.search("java.awt.list"));
		var expected = new HashSet<>(List.of("java.awt.List"));
		assertEquals(expected, actual);
	}

	@Test
	void search_no_results() throws Exception {
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
}

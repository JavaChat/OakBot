package oakbot.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oakbot.javadoc.ClassInfo;
import oakbot.javadoc.JavadocDao;
import oakbot.javadoc.JavadocLibrary;
import oakbot.javadoc.MultipleClassesFoundException;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocDaoTest {
	private final JavadocDao dao = new JavadocDao();
	{
		try {
			dao.addJavadocApi(new JavadocLibrary(null, null) {
				@Override
				public List<String> getAllClassNames() throws IOException {
					//@formatter:off
					return Arrays.asList(
						"javax.management.Attribute",
						"javax.naming.directory.Attribute",
						"java.lang.String"
					);
					//@formatter:on
				}

				@Override
				public ClassInfo getClassInfo(String className) throws IOException {
					if (className.startsWith("java.")) {
						return new ClassInfo(className, "description - " + className, null, Collections.emptyList(), false);
					}
					return null;
				}
			});
			dao.addJavadocApi(new JavadocLibrary(null, null) {
				@Override
				public List<String> getAllClassNames() throws IOException {
					//@formatter:off
					return Arrays.asList(
						"org.jsoup.nodes.Attribute"
					);
					//@formatter:on
				}

				@Override
				public ClassInfo getClassInfo(String className) throws IOException {
					if (className.startsWith("org.")) {
						return new ClassInfo(className, "description - " + className, null, Collections.emptyList(), false);
					}
					return null;
				}
			});
		} catch (IOException e) {
			//not thrown
		}
	}

	@Test
	public void simpleName_single_match() throws Exception {
		ClassInfo info = dao.getClassInfo("String");
		assertEquals("java.lang.String", info.getFullName());
		assertEquals("description - java.lang.String", info.getDescription());
	}

	@Test
	public void simpleName_no_match() throws Exception {
		ClassInfo info = dao.getClassInfo("FooBar");
		assertNull(info);

		info = dao.getClassInfo("freemarker.FooBar");
		assertNull(info);
	}

	@Test
	public void simpleName_case_insensitive() throws Exception {
		ClassInfo info = dao.getClassInfo("string");
		assertEquals("java.lang.String", info.getFullName());
		assertEquals("description - java.lang.String", info.getDescription());
	}

	@Test
	public void simpleName_multiple_matches() throws Exception {
		try {
			ClassInfo info = dao.getClassInfo("Attribute");
			fail(info.getFullName());
		} catch (MultipleClassesFoundException e) {
			Set<String> actual = new HashSet<>(e.getClasses());
			Set<String> expected = new HashSet<>(Arrays.asList("javax.management.Attribute", "javax.naming.directory.Attribute", "org.jsoup.nodes.Attribute"));
			assertEquals(expected, actual);
		}
	}

	@Test
	public void fullName() throws Exception {
		ClassInfo info = dao.getClassInfo("org.jsoup.nodes.Attribute");
		assertEquals("org.jsoup.nodes.Attribute", info.getFullName());
		assertEquals("description - org.jsoup.nodes.Attribute", info.getDescription());
	}

	@Test
	public void cache() throws Exception {
		JavadocLibrary spy = spy(new JavadocLibrary(null, null) {
			@Override
			public List<String> getAllClassNames() throws IOException {
				//@formatter:off
				return Arrays.asList(
					"java.lang.String"
				);
				//@formatter:on
			}

			@Override
			public ClassInfo getClassInfo(String className) throws IOException {
				if (className.startsWith("java.")) {
					return new ClassInfo(className, "description - " + className, null, Collections.emptyList(), false);
				}
				return null;
			}
		});

		JavadocDao dao = new JavadocDao();
		dao.addJavadocApi(spy);

		dao.getClassInfo("String");
		dao.getClassInfo("string");
		verify(spy, times(1)).getClassInfo("java.lang.String");
	}
}

package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocZipFileTest {
	private final JavadocZipFile zip;
	{
		try {
			zip = load("");
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void info_without_file() throws Exception {
		JavadocZipFile zip = load("-no-info");
		assertNull(zip.getName());
		assertNull(zip.getBaseUrl());
	}

	@Test
	public void info_without_attributes() throws Exception {
		JavadocZipFile zip = load("-no-attributes");
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
	public void getUrl() {
		ClassInfo info = new ClassInfo.Builder().name(new ClassName("java.util", "List")).build();
		assertEquals("https://docs.oracle.com/javase/8/docs/api/java/util/List.html", zip.getUrl(info, false));
		assertEquals("https://docs.oracle.com/javase/8/docs/api/index.html?java/util/List.html", zip.getUrl(info, true));
	}

	@Test
	public void getUrl_javadocUrlPattern() throws Exception {
		JavadocZipFile zip = load("-javadocUrlPattern");

		ClassInfo info = new ClassInfo.Builder().name(new ClassName("android.app", "Application")).build();
		assertEquals("http://developer.android.com/reference/android/app/Application.html", zip.getUrl(info, false));
		assertEquals("http://developer.android.com/reference/android/app/Application.html", zip.getUrl(info, true));
	}

	@Test
	public void getClassNames() throws Exception {
		Set<String> actual = new HashSet<>();
		for (ClassName className : zip.getClassNames()) {
			actual.add(className.getFullyQualifiedName());
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
		assertEquals("java.lang.Object", info.getName().getFullyQualifiedName());
		assertEquals("Object", info.getName().getSimpleName());
	}

	private static JavadocZipFile load(String suffix) throws IOException, URISyntaxException {
		URI uri = JavadocZipFileTest.class.getResource(JavadocZipFileTest.class.getSimpleName() + suffix + ".zip").toURI();
		Path file = Paths.get(uri);
		return new JavadocZipFile(file);
	}
}

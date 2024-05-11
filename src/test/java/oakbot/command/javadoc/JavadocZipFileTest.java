package oakbot.command.javadoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class JavadocZipFileTest {
	private final JavadocZipFile zip;
	{
		try {
			zip = load("");
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void info_without_file() throws Exception {
		var zip = load("-no-info");
		assertNull(zip.getName());
		assertNull(zip.getBaseUrl());
	}

	@Test
	void info_without_attributes() throws Exception {
		var zip = load("-no-attributes");
		assertNull(zip.getName());
		assertNull(zip.getBaseUrl());
	}

	@Test
	void info() {
		assertEquals("Java", zip.getName());
		assertEquals("8", zip.getVersion());
		assertEquals("https://docs.oracle.com/javase/8/docs/api/", zip.getBaseUrl());
		assertEquals("http://java.oracle.com", zip.getProjectUrl());
	}

	@Test
	void getUrl() {
		var info = new ClassInfo.Builder().name(new ClassName("java.util", "List")).build();
		assertEquals("https://docs.oracle.com/javase/8/docs/api/java/util/List.html", zip.getUrl(info, false));
		assertEquals("https://docs.oracle.com/javase/8/docs/api/index.html?java/util/List.html", zip.getUrl(info, true));
	}

	@Test
	void getUrl_javadocUrlPattern() throws Exception {
		var zip = load("-javadocUrlPattern");

		var info = new ClassInfo.Builder().name(new ClassName("android.app", "Application")).build();
		assertEquals("http://developer.android.com/reference/android/app/Application.html", zip.getUrl(info, false));
		assertEquals("http://developer.android.com/reference/android/app/Application.html", zip.getUrl(info, true));
	}

	@Test
	void getClassNames() throws Exception {
		//@formatter:off
		var actual = zip.getClassNames().stream()
			.map(ClassName::getFullyQualifiedName)
		.collect(Collectors.toSet());
		//@formatter:on

		//@formatter:off
		var expected = Set.of(
			"java.lang.Object",
			"java.awt.List",
			"java.util.List",
			"java.util.Collection"
		);
		//@formatter:on

		assertEquals(expected, actual);
	}

	@Test
	void getClassInfo_not_found() throws Exception {
		var info = zip.getClassInfo("java.lang.Foo");
		assertNull(info);
	}

	@Test
	void getClassInfo() throws Exception {
		var info = zip.getClassInfo("java.lang.Object");
		assertEquals("java.lang.Object", info.getName().getFullyQualifiedName());
		assertEquals("Object", info.getName().getSimpleName());
	}

	private static JavadocZipFile load(String suffix) throws IOException, URISyntaxException {
		var uri = JavadocZipFileTest.class.getResource(JavadocZipFileTest.class.getSimpleName() + suffix + ".zip").toURI();
		var file = Paths.get(uri);
		return new JavadocZipFile(file);
	}
}

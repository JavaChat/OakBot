package oakbot.command.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import oakbot.util.CloseableIterator;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class JavadocZipFileTest {
	private final Path root = Paths.get("src", "test", "resources", "oakbot", "command", "javadoc");
	private final JavadocZipFile zip;
	{
		Path file = root.resolve(getClass().getSimpleName() + ".zip");
		try {
			zip = new JavadocZipFile(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void info_without_file() throws Exception {
		Path file = root.resolve(getClass().getSimpleName() + "-no-info.zip");
		JavadocZipFile zip = new JavadocZipFile(file);
		assertNull(zip.getName());
		assertNull(zip.getBaseUrl());
	}

	@Test
	public void info_without_attributes() throws Exception {
		Path file = root.resolve(getClass().getSimpleName() + "-no-attributes.zip");
		JavadocZipFile zip = new JavadocZipFile(file);
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
		ClassInfo info = new ClassInfo.Builder().name("java.util.List", "List").build();
		assertEquals("https://docs.oracle.com/javase/8/docs/api/java/util/List.html", zip.getUrl(info, false));
		assertEquals("https://docs.oracle.com/javase/8/docs/api/index.html?java/util/List.html", zip.getUrl(info, true));
	}

	@Test
	public void getUrl_javadocUrlPattern() throws Exception {
		Path file = root.resolve(getClass().getSimpleName() + "-javadocUrlPattern.zip");
		JavadocZipFile zip = new JavadocZipFile(file);

		ClassInfo info = new ClassInfo.Builder().name("android.app.Application", "Application").build();
		assertEquals("http://developer.android.com/reference/android/app/Application.html", zip.getUrl(info, false));
		assertEquals("http://developer.android.com/reference/android/app/Application.html", zip.getUrl(info, true));
	}

	@Test
	public void getClasses() throws Exception {
		Set<String> actual = new HashSet<>();
		try (CloseableIterator<ClassName> it = zip.getClasses()) {
			while (it.hasNext()) {
				actual.add(it.next().getFullyQualified());
			}
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
		assertEquals("java.lang.Object", info.getName().getFullyQualified());
		assertEquals("Object", info.getName().getSimple());
	}
}

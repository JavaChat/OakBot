package oakbot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
public class PropertiesWrapperTest {
	@Test
	public void constructor_file_not_found() throws Exception {
		assertThrows(IOException.class, () -> new PropertiesWrapper(Paths.get("foo")));
	}

	@Test
	public void get() {
		var props = new Properties();
		props.setProperty("key1", "value1");
		props.setProperty("key2", " value2 ");
		props.setProperty("key3", "");
		props.setProperty("key4", " ");

		var wrapper = new PropertiesWrapper(props);
		assertEquals("value1", wrapper.get("key1"));
		assertEquals("value2", wrapper.get("key2"));
		assertNull(wrapper.get("key3"));
		assertNull(wrapper.get("key4"));
		assertNull(wrapper.get("does-not-exist"));
	}

	@Test
	public void getPath() {
		var props = new Properties();
		props.setProperty("key1", "value1");
		props.setProperty("key2", " value2 ");
		props.setProperty("key3", "");
		props.setProperty("key4", " ");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(Paths.get("value1"), wrapper.getPath("key1"));
		assertEquals(Paths.get("value2"), wrapper.getPath("key2"));
		assertNull(wrapper.getPath("key3"));
		assertNull(wrapper.getPath("key4"));
		assertNull(wrapper.getPath("does-not-exist"));
	}

	@Test
	public void getInteger() {
		var props = new Properties();
		props.setProperty("key", "1");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(Integer.valueOf(1), wrapper.getInteger("key"));

		assertNull(wrapper.getInteger("foo"));
		assertEquals(Integer.valueOf(1), wrapper.getInteger("foo", 1));
	}

	@Test
	public void getInteger_invalid() {
		Properties props = new Properties();
		props.setProperty("key", "value");

		var wrapper = new PropertiesWrapper(props);
		assertThrows(NumberFormatException.class, () -> wrapper.getInteger("key"));
	}

	@Test
	public void getDate() throws Exception {
		var props = new Properties();
		props.setProperty("key", "2015-04-11 15:43:00");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(date("2015-04-11 15:43:00"), wrapper.getDate("key"));

		assertNull(wrapper.getDate("foo"));
	}

	@Test
	public void getDate_invalid() throws Exception {
		var props = new Properties();
		props.setProperty("key", "value");

		var wrapper = new PropertiesWrapper(props);
		assertThrows(DateTimeParseException.class, () -> wrapper.getDate("key"));
	}

	@Test
	public void getBoolean() {
		var props = new Properties();
		props.setProperty("key1", "true");
		props.setProperty("key2", "false");
		props.setProperty("key3", "foo");

		var wrapper = new PropertiesWrapper(props);
		assertTrue(wrapper.getBoolean("key1"));
		assertFalse(wrapper.getBoolean("key2"));
		assertFalse(wrapper.getBoolean("key3"));

		assertNull(wrapper.getBoolean("foo"));
		assertTrue(wrapper.getBoolean("foo", true));
	}

	@Test
	public void getFile() {
		var props = new Properties();
		props.setProperty("key", "path/to/file");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(Paths.get("path", "to", "file"), wrapper.getFile("key"));

		assertNull(wrapper.getFile("foo"));
	}

	@Test
	public void set() {
		var wrapper = new PropertiesWrapper();
		var value = mock(Object.class);
		doReturn("foo").when(value).toString();
		wrapper.set("key", value);

		assertEquals("foo", wrapper.get("key"));
	}

	@Test
	public void set_date() throws Exception {
		var wrapper = new PropertiesWrapper();
		var value = date("2015-11-04");
		wrapper.set("key", value);

		assertEquals(value, wrapper.getDate("key"));
	}

	@Test
	public void getIntegerList() {
		var props = new Properties();
		props.setProperty("key1", "1");
		props.setProperty("key2", "2,3 , 4");
		props.setProperty("key3", "2,foo");
		props.setProperty("key4", "");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(List.of(1), wrapper.getIntegerList("key1"));
		assertEquals(List.of(2, 3, 4), wrapper.getIntegerList("key2"));
		assertEquals(List.of(2), wrapper.getIntegerList("key3"));
		assertEquals(List.of(), wrapper.getIntegerList("key4"));
		assertEquals(List.of(), wrapper.getIntegerList("foo"));
	}

	private static LocalDateTime date(String date) {
		if (date.length() == 10) {
			date += " 00:00:00";
		}

		try {
			var df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			return LocalDateTime.parse(date, df);
		} catch (DateTimeParseException e) {
			throw new RuntimeException(e);
		}
	}
}

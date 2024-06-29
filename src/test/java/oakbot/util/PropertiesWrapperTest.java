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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Angstadt
 */
class PropertiesWrapperTest {
	@Test
	void constructor_file_not_found() {
		assertThrows(IOException.class, () -> new PropertiesWrapper(Paths.get("foo")));
	}

	@Test
	void get() {
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
	void getPath() {
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
	void getDuration() {
		var props = new Properties();
		props.setProperty("key1", "PT1H");
		props.setProperty("key2", " PT1H ");
		props.setProperty("key3", "");
		props.setProperty("key4", " ");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(Duration.ofHours(1), wrapper.getDuration("key1"));
		assertEquals(Duration.ofHours(1), wrapper.getDuration("key2"));
		assertNull(wrapper.getDuration("key3"));
		assertNull(wrapper.getDuration("key4"));
		assertNull(wrapper.getDuration("does-not-exist"));
	}

	@Test
	void getInteger() {
		var props = new Properties();
		props.setProperty("key", "1");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(Integer.valueOf(1), wrapper.getInteger("key"));

		assertNull(wrapper.getInteger("foo"));
		assertEquals(Integer.valueOf(1), wrapper.getInteger("foo", 1));
	}

	@Test
	void getInteger_invalid() {
		Properties props = new Properties();
		props.setProperty("key", "value");

		var wrapper = new PropertiesWrapper(props);
		assertThrows(NumberFormatException.class, () -> wrapper.getInteger("key"));
	}

	@Test
	void getDate() throws Exception {
		var props = new Properties();
		props.setProperty("key", "2015-04-11 15:43:00");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(date("2015-04-11 15:43:00"), wrapper.getDate("key"));

		assertNull(wrapper.getDate("foo"));
	}

	@Test
	void getDate_invalid() {
		var props = new Properties();
		props.setProperty("key", "value");

		var wrapper = new PropertiesWrapper(props);
		assertThrows(DateTimeParseException.class, () -> wrapper.getDate("key"));
	}

	@Test
	void getBoolean() {
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
	void getFile() {
		var props = new Properties();
		props.setProperty("key", "path/to/file");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(Paths.get("path", "to", "file"), wrapper.getFile("key"));

		assertNull(wrapper.getFile("foo"));
	}

	@Test
	void set() {
		var wrapper = new PropertiesWrapper();
		var value = mock(Object.class);
		doReturn("foo").when(value).toString();
		wrapper.set("key", value);

		assertEquals("foo", wrapper.get("key"));
	}

	@Test
	void set_date() throws Exception {
		var wrapper = new PropertiesWrapper();
		var value = date("2015-11-04");
		wrapper.set("key", value);

		assertEquals(value, wrapper.getDate("key"));
	}

	@Test
	void getIntegerList() {
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

	@Test
	void getLongList() {
		var props = new Properties();
		props.setProperty("key1", "1");
		props.setProperty("key2", "2,3 , 4");
		props.setProperty("key3", "2,foo");
		props.setProperty("key4", "");

		var wrapper = new PropertiesWrapper(props);
		assertEquals(List.of(1L), wrapper.getLongList("key1"));
		assertEquals(List.of(2L, 3L, 4L), wrapper.getLongList("key2"));
		assertEquals(List.of(2L), wrapper.getLongList("key3"));
		assertEquals(List.of(), wrapper.getLongList("key4"));
		assertEquals(List.of(), wrapper.getLongList("foo"));
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

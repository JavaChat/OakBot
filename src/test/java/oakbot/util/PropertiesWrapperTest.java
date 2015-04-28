package oakbot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class PropertiesWrapperTest {
	@Test(expected = IOException.class)
	public void constructor_file_not_found() throws Exception {
		new PropertiesWrapper(Paths.get("foo"));
	}

	@Test
	public void get() {
		Properties props = new Properties();
		props.setProperty("key", "value");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		assertEquals("value", wrapper.get("key"));
		assertNull(wrapper.get("does-not-exist"));
	}

	@Test
	public void getInteger() {
		Properties props = new Properties();
		props.setProperty("key", "1");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		assertEquals(Integer.valueOf(1), wrapper.getInteger("key"));

		assertNull(wrapper.getInteger("foo"));
		assertEquals(Integer.valueOf(1), wrapper.getInteger("foo", 1));
	}

	@Test(expected = NumberFormatException.class)
	public void getInteger_invalid() {
		Properties props = new Properties();
		props.setProperty("key", "value");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		wrapper.getInteger("key");
	}

	@Test
	public void getDate() throws Exception {
		Properties props = new Properties();
		props.setProperty("key", "2015-04-11 15:43:00");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		assertEquals(date("2015-04-11 15:43:00"), wrapper.getDate("key"));

		assertNull(wrapper.getDate("foo"));
	}

	@Test(expected = ParseException.class)
	public void getDate_invalid() throws Exception {
		Properties props = new Properties();
		props.setProperty("key", "value");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		wrapper.getDate("key");
	}

	@Test
	public void getBoolean() {
		Properties props = new Properties();
		props.setProperty("key1", "true");
		props.setProperty("key2", "false");
		props.setProperty("key3", "foo");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		assertTrue(wrapper.getBoolean("key1"));
		assertFalse(wrapper.getBoolean("key2"));
		assertFalse(wrapper.getBoolean("key3"));

		assertNull(wrapper.getBoolean("foo"));
		assertTrue(wrapper.getBoolean("foo", true));
	}

	@Test
	public void getFile() {
		Properties props = new Properties();
		props.setProperty("key", "path/to/file");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		assertEquals(Paths.get("path", "to", "file"), wrapper.getFile("key"));

		assertNull(wrapper.getFile("foo"));
	}

	@Test
	public void set() {
		PropertiesWrapper wrapper = new PropertiesWrapper();
		Object value = mock(Object.class);
		doReturn("foo").when(value).toString();
		wrapper.set("key", value);

		assertEquals("foo", wrapper.get("key"));
	}

	@Test
	public void set_date() throws Exception {
		PropertiesWrapper wrapper = new PropertiesWrapper();
		Date value = date("2015-11-04");
		wrapper.set("key", value);

		assertEquals(value, wrapper.getDate("key"));
	}

	@Test
	public void getIntegerList() {
		Properties props = new Properties();
		props.setProperty("key1", "1");
		props.setProperty("key2", "2,3 , 4");
		props.setProperty("key3", "2,foo");
		props.setProperty("key4", "");

		PropertiesWrapper wrapper = new PropertiesWrapper(props);
		assertEquals(Arrays.asList(1), wrapper.getIntegerList("key1"));
		assertEquals(Arrays.asList(2, 3, 4), wrapper.getIntegerList("key2"));
		try {
			wrapper.getIntegerList("key3");
			fail();
		} catch (NumberFormatException e) {
			//expected
		}
		try {
			wrapper.getIntegerList("key4");
			fail();
		} catch (NumberFormatException e) {
			//expected
		}
		assertEquals(Arrays.asList(), wrapper.getIntegerList("foo"));
	}

	private static Date date(String date) {
		if (date.length() == 10) {
			date += " 00:00:00";
		}

		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return df.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}

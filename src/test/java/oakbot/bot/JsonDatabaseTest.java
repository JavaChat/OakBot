package oakbot.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Files;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import oakbot.JsonDatabase;

/**
 * @author Michael Angstadt
 */
public class JsonDatabaseTest {
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void round_trip() throws Exception {
		var file = temp.newFile().toPath();
		Files.delete(file);

		Map<String, Object> map = new HashMap<>();
		{
			map.put("one", "One");
			map.put("two", "Two");
			map.put("three", 3);

			Map<String, Object> submap = new HashMap<>();
			submap.put("five", "5");
			map.put("four", Arrays.asList(1, "2", null, List.of(3, 4), submap));

			map.put("five", date("2017-03-26 00:00:00"));
			map.put("six", null);
			map.put("seven", Integer.MAX_VALUE + 1L); //long value
		}

		var list = List.of(1, 2);
		var value = "three";

		var db = new JsonDatabase(file);
		db.set("map", map);
		db.set("list", list);
		db.set("value", value);
		db.commit();

		//System.out.println(new String(Files.readAllBytes(file)));

		db = new JsonDatabase(file);
		assertEquals(map, db.get("map"));
		assertEquals(list, db.get("list"));
		assertEquals(value, db.get("value"));
	}

	@Test
	public void non_existant_key() throws Exception {
		var file = temp.newFile().toPath();
		Files.delete(file);

		var db = new JsonDatabase(file);
		assertNull(db.get("non-existant"));
	}

	private static LocalDateTime date(String date) throws ParseException {
		var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return LocalDateTime.parse(date, formatter);
	}
}

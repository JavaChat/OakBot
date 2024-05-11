package oakbot.command.learn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oakbot.Database;

/**
 * @author Michael Angstadt
 */
class LearnedCommandsDaoTest {
	@Test
	void non_existant_command() {
		var dao = new LearnedCommandsDao();
		assertFalse(dao.contains("foo"));
		assertNull(dao.get("foo"));
		assertFalse(dao.remove("foo"));
	}

	@Test
	void add_and_remove() {
		var db = mock(Database.class);
		var dao = new LearnedCommandsDao(db);
		var now = LocalDateTime.now();

		//@formatter:off
		dao.add(new LearnedCommand.Builder()
			.authorUserId(100)
			.authorUsername("Username")
			.roomId(1)
			.messageId(1000L)
			.created(now)
			.name("name")
			.output("output")
		.build());

		var dbValue = new ArrayList<Object>();

		dbValue.add(Map.of(
			"authorUserId", 100,
			"authorUsername", "Username",
			"roomId", 1,
			"messageId", 1000L,
			"created", now,
			"name", "name",
			"output", "output"
		));

		verify(db).set("learned-commands", dbValue);

		dao.add(new LearnedCommand.Builder()
			.authorUserId(101)
			.authorUsername("Username2")
			.roomId(2)
			.messageId(1001L)
			.created(now)
			.name("name2")
			.output("output2")
		.build());

		dbValue.add(Map.of(
			"authorUserId", 101,
			"authorUsername", "Username2",
			"roomId", 2,
			"messageId", 1001L,
			"created", now,
			"name", "name2",
			"output", "output2"
		));
		//@formatter:on

		verify(db).set("learned-commands", dbValue);

		dao.remove("name");
		dbValue.remove(0);

		verify(db).set("learned-commands", dbValue);
	}

	@Test
	void load_from_empty_database() {
		{
			var db = mock(Database.class);
			when(db.getList("learned-commands")).thenReturn(null);
			var dao = new LearnedCommandsDao(db);
			assertFalse(dao.iterator().hasNext());
		}

		{
			var db = mock(Database.class);
			when(db.getList("learned-commands")).thenReturn(Collections.emptyList());
			var dao = new LearnedCommandsDao(db);
			assertFalse(dao.iterator().hasNext());
		}
	}

	@Test
	void load_from_database() {
		LocalDateTime now = LocalDateTime.now();
		List<Object> dbValue = new ArrayList<>();

		Map<String, Object> map = new HashMap<>();
		map.put("authorUserId", 100);
		map.put("authorUsername", "Username");
		map.put("roomId", 1);
		map.put("messageId", 1000);
		map.put("created", now);
		map.put("name", "name");
		map.put("output", "output");
		dbValue.add(map);

		map = new HashMap<>();
		map.put("authorUserId", 101);
		map.put("authorUsername", "Username2");
		map.put("roomId", 2);
		map.put("messageId", Integer.MAX_VALUE + 1L); //long value returned
		map.put("created", now);
		map.put("name", "name2");
		map.put("output", "output2");
		dbValue.add(map);

		/*
		 * Older learned commands will not have extra metadata.
		 */
		map = new HashMap<>();
		map.put("name", "name3");
		map.put("output", "output3");
		dbValue.add(map);

		Database db = mock(Database.class);
		when(db.getList("learned-commands")).thenReturn(dbValue);
		LearnedCommandsDao dao = new LearnedCommandsDao(db);

		Iterator<LearnedCommand> it = dao.iterator();

		LearnedCommand command = it.next();
		assertEquals(Integer.valueOf(100), command.getAuthorUserId());
		assertEquals("Username", command.getAuthorUsername());
		assertEquals(Integer.valueOf(1), command.getRoomId());
		assertEquals(Long.valueOf(1000), command.getMessageId());
		assertEquals(now, command.getCreated());
		assertEquals("name", command.name());
		assertTrue(command.aliases().isEmpty());
		assertEquals("output", command.getOutput());
		assertSame(command, dao.get("name"));
		assertTrue(dao.contains("name"));

		command = it.next();
		assertEquals(Integer.valueOf(101), command.getAuthorUserId());
		assertEquals("Username2", command.getAuthorUsername());
		assertEquals(Integer.valueOf(2), command.getRoomId());
		assertEquals(Long.valueOf(Integer.MAX_VALUE + 1L), command.getMessageId());
		assertEquals(now, command.getCreated());
		assertEquals("name2", command.name());
		assertTrue(command.aliases().isEmpty());
		assertEquals("output2", command.getOutput());
		assertSame(command, dao.get("name2"));
		assertTrue(dao.contains("name2"));

		command = it.next();
		assertNull(command.getAuthorUserId());
		assertNull(command.getAuthorUsername());
		assertNull(command.getRoomId());
		assertNull(command.getMessageId());
		assertNull(command.getCreated());
		assertEquals("name3", command.name());
		assertTrue(command.aliases().isEmpty());
		assertEquals("output3", command.getOutput());
		assertSame(command, dao.get("name3"));
		assertTrue(dao.contains("name3"));

		assertFalse(it.hasNext());
	}
}

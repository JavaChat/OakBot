package oakbot.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
public class CommandTest {
	@Test
	public void isInvokingMe() {
		Command command = new Command() {
			@Override
			public String name() {
				return "name";
			}

			@Override
			public List<String> aliases() {
				return List.of("one", "two");
			}

			@Override
			public HelpDoc help() {
				return null;
			}

			@Override
			public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
				return null;
			}
		};

		assertIsInvokingMe(command, "/name");
		assertIsInvokingMe(command, "/name foo");
		assertIsInvokingMe(command, "/one");
		assertIsInvokingMe(command, "/one foo");
		assertNotInvokingMe(command, "/three");
		assertNotInvokingMe(command, "blah");
	}

	private static void assertIsInvokingMe(Command command, String content) {
		ChatMessage message = new ChatMessage.Builder().content(content).build();
		assertTrue(command.isInvokingMe(message, "/"));
	}

	private static void assertNotInvokingMe(Command command, String content) {
		ChatMessage message = new ChatMessage.Builder().content(content).build();
		assertFalse(command.isInvokingMe(message, "/"));
	}
}

package oakbot.command.learn;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class LearnedCommandTest {
	@Test
	public void onMessage() {
		//@formatter:off
		LearnedCommand cmd = new LearnedCommand.Builder()
			.name("complement")
			.output("{0} is {1}!")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);
		
		//no arguments

		//@formatter:off
		ChatCommand chatCommand = new ChatCommandBuilder(cmd)
			.content("")
		.build();
		//@formatter:on

		ChatActions actions = cmd.onMessage(chatCommand, bot);
		assertMessage("{0} is {1}!", actions);
		
		//two arguments

		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("Michael awesome")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Michael is awesome!", actions);
		
		//quoted argument

		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("\"Oak Bot\" awesome")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Oak Bot is awesome!", actions);
		
		//escaped quotes

		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("\"Oak \\\"Bot\\\"\" awesome")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Oak \"Bot\" is awesome!", actions);
		
		//two quoted arguments

		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("\"Oak Bot\" \"very cool\"")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Oak Bot is very cool!", actions);
		
		//too many arguments
		
		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("Michael awesome cool")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Michael is awesome!", actions);
		
		//not enough arguments
		
		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("Michael")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Michael is {1}!", actions);
		
		//escaped space
		
		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("Michael\\ awesome")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage("Michael awesome is {1}!", actions);
		
		//empty argument
		
		//@formatter:off
		chatCommand = new ChatCommandBuilder(cmd)
			.content("\"\"")
		.build();
		//@formatter:on

		actions = cmd.onMessage(chatCommand, bot);
		assertMessage(" is {1}!", actions);
	}
}

package oakbot.command.effective;

import static oakbot.bot.ChatActions.reply;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import oakbot.bot.BotContext;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.PostMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays items from the book "Effective Debugging, 66 Specific Ways to Debug
 * Software and Systems" by Diomidis Spinellis.
 * @author Michael Angstadt
 */
public class EffectiveDebuggingCommand implements Command {
	final List<Item> items = Arrays.asList( //@formatter:off
		new Item.Builder().number(1).title("Handle All Problems through an Issue-Tracking System").page(1).build(),
        new Item.Builder().number(2).title("Use Focused Queries to Search the Web for Insights into Your Problem").page(3).build(),
        new Item.Builder().number(3).title("Confirm That Preconditions and Postconditions Are Satisfied").page(5).build(),
        new Item.Builder().number(4).title("Drill Up from the Problem to the Bug or Down from the Program's Start to the Bug").page(7).build(),
        new Item.Builder().number(5).title("Find the Difference between a Known Good System and a Failing One").page(9).build(),
        new Item.Builder().number(6).title("Use the Software's Debugging Facilities").page(12).build(),
        new Item.Builder().number(7).title("Diversify Your Build and Execution Environment").page(17).build(),
        new Item.Builder().number(8).title("Focus Your Work on the Most Important Problems").page(20).build(),
        
        new Item.Builder().number(9).title("Set Yourself Up for Debugging Success").page(23).build(),
        new Item.Builder().number(10).title("Enable the Efficient Reproduction of the Problem").page(25).build(),
        new Item.Builder().number(11).title("Minimize the Turnaround Time from Your Changes to Their Result").page(28).build(),
        new Item.Builder().number(12).title("Automate Complex Testing Scenarios").page(29).build(),
        new Item.Builder().number(13).title("Enable a Comprehensive Overview of Your Debugging Data").page(32).build(),
        new Item.Builder().number(14).title("Consider Updating Your Software").page(33).build(),
        new Item.Builder().number(15).title("Consult Third-Party Source Code for Insights on Its Use").page(34).build(),
        new Item.Builder().number(16).title("Use Specialized Monitoring and Test Equipment").page(36).build(),
        new Item.Builder().number(17).title("Increase the Prominence of a Failure's Effects").page(40).build(),
        new Item.Builder().number(18).title("Enable the Debugging of Unwieldy Systems from Your Desk").page(42).build(),
        new Item.Builder().number(19).title("Automate Debugging Tasks").page(44).build(),
        new Item.Builder().number(20).title("Houseclean Before and After Debugging").page(45).build(),
        new Item.Builder().number(21).title("Fix All Instances of a Problem Class").page(46).build(),
        
        new Item.Builder().number(22).title("Analyze Debug Data with Unix Command-Line Tools").page(49).build(),
        new Item.Builder().number(23).title("Utilize Command-Line Tool Options and Idioms").page(55).build(),
        new Item.Builder().number(24).title("Explore Debug Data with Your Editor").page(57).build(),
        new Item.Builder().number(25).title("Optimize Your Work Environment").page(59).build(),
        new Item.Builder().number(26).title("Hunt the Causes and History of Bugs with the Revision Control System").page(64).build(),
        new Item.Builder().number(27).title("Use Monitoring Tools on Systems Composed of Independent Processes").page(67).build(),
        
        new Item.Builder().number(28).title("Use Code Compiled for Symbolic Debugging").page(71).build(),
        new Item.Builder().number(29).title("Step through the Code").page(76).build(),
        new Item.Builder().number(30).title("Use Code and Data Breakpoints").page(77).build(),
        new Item.Builder().number(31).title("Familiarize Yourself with Reverse Debugging").page(80).build(),
        new Item.Builder().number(32).title("Navigate along the Calls between Routines").page(82).build(),
        new Item.Builder().number(33).title("Look for Errors by Examining the Values of Variables and Expressions").page(84).build(),
        new Item.Builder().number(34).title("Know How to Attach a Debugger to a Running Process").page(87).build(),
        new Item.Builder().number(35).title("Know How to Work with Core Dumps").page(89).build(),
        new Item.Builder().number(36).title("Tune Your Debugging Tools").page(92).build(),
        new Item.Builder().number(37).title("Know How to View Assembly Code and Raw Memory").page(95).build(),
        
        new Item.Builder().number(38).title("Review and Manually Execute Suspect Code").page(101).build(),
        new Item.Builder().number(39).title("Go Over Your Code and Reasoning with a Colleague").page(103).build(),
        new Item.Builder().number(40).title("Add Debugging Functionality").page(104).build(),
        new Item.Builder().number(41).title("Add Logging Statements").page(108).build(),
        new Item.Builder().number(42).title("Use Unit Tests").page(112).build(),
        new Item.Builder().number(43).title("Use Assertions").page(116).build(),
        new Item.Builder().number(44).title("Verify Your Reasoning by Perturbing the Debugged Program").page(119).build(),
        new Item.Builder().number(45).title("Minimize the Differences between a Working Example and the Failing Code").page(120).build(),
        new Item.Builder().number(46).title("Simplify the Suspect Code").page(121).build(),
        new Item.Builder().number(47).title("Consider Rewriting the Suspect Code in Another Language").page(124).build(),
        new Item.Builder().number(48).title("Improve the Suspect Code's Readability and Structure").page(126).build(),
        new Item.Builder().number(49).title("Fix the Bug's Cause, Rather Than Its Symptom").page(129).build(),
        
        new Item.Builder().number(50).title("Examine Generated Code").page(133).build(),
        new Item.Builder().number(51).title("Use Static Program Analysis").page(136).build(),
        new Item.Builder().number(52).title("Configure Deterministic Builds and Executions").page(141).build(),
        new Item.Builder().number(53).title("Configure the Use of Debugging Libraries and Checks").page(143).build(),
        
        new Item.Builder().number(54).title("Find the Fault by Constructing a Test Case").page(149).build(),
        new Item.Builder().number(55).title("Fail Fast").page(153).build(),
        new Item.Builder().number(56).title("Examine Application Log Files").page(154).build(),
        new Item.Builder().number(57).title("Profile the Operation of Systems and Processes").page(158).build(),
        new Item.Builder().number(58).title("Trace the Code's Execution").page(162).build(),
        new Item.Builder().number(59).title("Use Dynamic Program Analysis Tools").page(168).build(),
        
        new Item.Builder().number(60).title("Analyze Deadlocks with Postmortem Debugging").page(171).build(),
        new Item.Builder().number(61).title("Capture and Replicate").page(178).build(),
        new Item.Builder().number(62).title("Uncover Deadlocks and Race Conditions with Specialized Tools").page(183).build(),
        new Item.Builder().number(63).title("Isolate and Remove Nondeterminism").page(188).build(),
        new Item.Builder().number(64).title("Investigate Scalability Issues by Looking at Contention").page(190).build(),
        new Item.Builder().number(65).title("Locate False Sharing by Using Performance Counters").page(193).build(),
        new Item.Builder().number(66).title("Consider Rewriting the Code Using Higher-Level Abstractions").page(197).build()
	); //@formatter:on

	@Override
	public String name() {
		return "ed";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays items from the book \"Effective Debugging, 66 Specific Ways to Debug Software and Systems\" by Diomidis Spinellis.")
			.example("!list", "Lists all items.")
			.example("!random", "Displays a random item.")
			.example("5", "Displays item #5.")
			.example("automate", "Displays all items that contain the keyword \"automate\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent();

		/*
		 * Display the help text.
		 */
		if (content.isEmpty()) {
			return reply(help().getHelpText(context.getTrigger()), chatCommand);
		}

		/*
		 * Display all the items.
		 */
		if ("!list".equalsIgnoreCase(content)) {
			return displayItems(chatCommand, items);
		}

		/*
		 * Display a random item.
		 */
		if ("!random".equalsIgnoreCase(content)) {
			int itemNumber = ThreadLocalRandom.current().nextInt(items.size());
			Item item = items.get(itemNumber);
			return displayItem(chatCommand, item);
		}

		/*
		 * Display item by number.
		 */
		try {
			int itemNumber = Integer.parseInt(content);
			if (itemNumber <= 0) {
				return reply("Item number must be greater than 0.", chatCommand);
			}
			if (itemNumber > items.size()) {
				return reply("There are only " + items.size() + " items.", chatCommand);
			}

			Item item = items.get(itemNumber - 1);
			return displayItem(chatCommand, item);
		} catch (NumberFormatException e) {
			//user did not enter an item number
		}

		/*
		 * Search by keyword.
		 */
		String searchKeyword = content.toLowerCase();
		List<Item> searchResults = items.stream() //@formatter:off
			.filter(item -> item.title.toLowerCase().contains(searchKeyword))
		.collect(Collectors.toList()); //@formatter:on

		/*
		 * No search results found.
		 */
		if (searchResults.isEmpty()) {
			return reply("No matches found.", chatCommand);
		}

		/*
		 * One item found.
		 */
		if (searchResults.size() == 1) {
			return displayItem(chatCommand, searchResults.get(0));
		}

		/*
		 * Multiple items found.
		 */
		return displayItems(chatCommand, searchResults);
	}

	private ChatActions displayItem(ChatCommand chatCommand, Item item) {
		return displayItems(chatCommand, Arrays.asList(item));
	}

	private ChatActions displayItems(ChatCommand chatCommand, List<Item> items) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(chatCommand);
		for (Item item : items) {
			cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title)).append(" (p. ").append(item.page).append(")").nl();
		}
		cb.append("(source: Effective Debugging, 66 Specific Ways to Debug Software and Systems by Diomidis Spinellis)");

		return ChatActions.create( //@formatter:off
			new PostMessage(cb).splitStrategy(SplitStrategy.NEWLINE)
		); //@formatter:on
	}

	private static String removeMarkdown(String s) {
		return s.replaceAll("[`*]", "");
	}

	static class Item {
		final int number, page;
		final String title;

		private Item(Builder builder) {
			number = builder.number;
			page = builder.page;
			title = builder.title;
		}

		public static class Builder {
			private int number, page;
			private String title;

			public Builder number(int number) {
				this.number = number;
				return this;
			}

			public Builder page(int page) {
				this.page = page;
				return this;
			}

			public Builder title(String title) {
				this.title = title;
				return this;
			}

			public Item build() {
				return new Item(this);
			}
		}
	}
}

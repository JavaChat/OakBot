package oakbot.command.effective;

import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.xml.sax.SAXException;

import com.github.mangstadt.sochat4j.SplitStrategy;
import com.github.mangstadt.sochat4j.util.Leaf;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays items from the book "Effective Java, Third Edition" by Joshua Bloch.
 * @author Michael Angstadt
 */
public class EffectiveJavaCommand implements Command {
	private final List<Item> items;

	public EffectiveJavaCommand() {
		Leaf document;
		try (var in = getClass().getResourceAsStream("effective-java.xml")) {
			document = Leaf.parse(in);
		} catch (IOException | SAXException ignored) {
			/*
			 * These exceptions should never be thrown because the XML file is
			 * on the classpath and is not coming from user input.
			 * 
			 * The XML file is also checked for correctness in
			 * EffectiveJavaXmlTest.
			 */
			throw new RuntimeException(ignored);
		}

		var itemsByNumber = new HashMap<Integer, Item>();
		var itemElements = document.select("/items/item");
		for (var itemElement : itemElements) {
			var number = Integer.parseInt(itemElement.attribute("number"));
			var page = Integer.parseInt(itemElement.attribute("page"));
			var title = itemElement.selectFirst("title").text().trim();

			//@formatter:off
			var summary = itemElement.selectFirst("summary").text().trim()
				/*
				 * Remove any XML indentation whitespace at the beginning of
				 * each line.
				 */
				.replaceAll("(\r\n|\r|\n)\\s+", "$1");

			var item = new Item.Builder()
				.number(number)
				.page(page)
				.title(title)
				.summary(summary)
			.build();
			//@formatter:on

			itemsByNumber.put(item.number, item);
		}

		//@formatter:off
		items = IntStream.rangeClosed(1, itemsByNumber.size())
			.mapToObj(itemsByNumber::get)
		.toList();
		//@formatter:on
	}

	@Override
	public String name() {
		return "ej";
	}

	@Override
	public List<String> aliases() {
		return List.of("bloch");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays items from the book \"Effective Java, Third Edition\" by Joshua Bloch.")
			.example("!list", "Lists all items.")
			.example("!random", "Displays a random item.")
			.example("5", "Displays item #5.")
			.example("string", "Displays all items that contain the keyword \"string\".")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent();

		/*
		 * Display the help text.
		 */
		if (content.isEmpty()) {
			return reply(help().getHelpText(bot.getTrigger()), chatCommand);
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
			var itemNumber = ThreadLocalRandom.current().nextInt(items.size());
			var item = items.get(itemNumber);
			return displayItem(chatCommand, item);
		}

		/*
		 * Display item by number.
		 */
		try {
			var itemNumber = Integer.parseInt(content);
			if (itemNumber <= 0) {
				return reply("Item number must be greater than 0.", chatCommand);
			}
			if (itemNumber > items.size()) {
				return reply("There are only " + items.size() + " items.", chatCommand);
			}

			var item = items.get(itemNumber - 1);
			return displayItem(chatCommand, item);
		} catch (NumberFormatException e) {
			//user did not enter an item number
		}

		/*
		 * Search by keyword.
		 */
		var contentToLower = content.toLowerCase();

		//@formatter:off
		var searchResults = items.stream().filter(item -> 
			item.title.toLowerCase().contains(contentToLower) ||
			item.summary.toLowerCase().contains(contentToLower)
		).toList();
		//@formatter:on

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
		var cb = new ChatBuilder();
		cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title));
		cb.nl().append(removeMarkdown(item.summary));
		cb.nl().append("(source: Effective Java, Third Edition by Joshua Bloch, p.").append(item.page).append(")");

		return reply(cb, chatCommand, SplitStrategy.WORD);
	}

	private ChatActions displayItems(ChatCommand chatCommand, List<Item> items) {
		var cb = new ChatBuilder();
		for (var item : items) {
			cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title)).nl();
		}

		return reply(cb, chatCommand, SplitStrategy.NEWLINE);
	}

	private static String removeMarkdown(String s) {
		return s.replaceAll("[`*]", "");
	}

	private static class Item {
		private final int number;
		private final int page;
		private final String title;
		private final String summary;

		private Item(Builder builder) {
			number = builder.number;
			page = builder.page;
			title = builder.title;
			summary = builder.summary;
		}

		public static class Builder {
			private int number;
			private int page;
			private String title;
			private String summary;

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

			public Builder summary(String summary) {
				this.summary = summary;
				return this;
			}

			public Item build() {
				return new Item(this);
			}
		}
	}
}

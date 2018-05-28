package oakbot.command.effective;

import static oakbot.command.Command.reply;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.SplitStrategy;
import oakbot.command.Command;
import oakbot.util.ChatBuilder;
import oakbot.util.Leaf;

/**
 * Displays items from the book "Effective Java, Third Edition" by Joshua Bloch.
 * @author Michael Angstadt
 */
public class EffectiveJavaCommand implements Command {
	private final List<Item> items;

	public EffectiveJavaCommand() {
		//NOTE: The XML file is checked for correctness in EffectiveJavaXmlTest.
		Leaf document;
		try (InputStream in = getClass().getResourceAsStream("effective-java.xml")) {
			document = new Leaf(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in));
		} catch (IOException | SAXException | ParserConfigurationException ignored) {
			/*
			 * These exceptions should never be thrown because the XML file is
			 * on the classpath and is not coming from user input.
			 */
			throw new RuntimeException(ignored);
		}

		Map<Integer, Item> itemsByNumber = new HashMap<>();
		List<Leaf> itemElements = document.select("/items/item");
		for (Leaf itemElement : itemElements) {
			int number = Integer.parseInt(itemElement.attribute("number"));
			int page = Integer.parseInt(itemElement.attribute("page"));
			String title = itemElement.selectFirst("title").text().trim();

			String summary;
			{
				Leaf summaryElement = itemElement.selectFirst("summary");
				summary = summaryElement.text().trim();

				/**
				 * Remove any XML indentation whitespace at the beginning of
				 * each line.
				 */
				summary = summary.replaceAll("(\r\n|\r|\n)\\s+", "$1");
			}

			//@formatter:off
			Item item = new Item.Builder()
				.number(number)
				.page(page)
				.title(title)
				.summary(summary)
			.build();
			//@formatter:on

			itemsByNumber.put(item.number, item);
		}

		items = new ArrayList<>(itemsByNumber.size());
		for (int i = 1; i <= itemsByNumber.size(); i++) {
			Item item = itemsByNumber.get(i);
			items.add(item);
		}
	}

	@Override
	public String name() {
		return "ej";
	}

	@Override
	public List<String> aliases() {
		return Arrays.asList("bloch");
	}

	@Override
	public String description() {
		return "Displays items from the book \"Effective Java, Third Edition\" by Joshua Bloch.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append(description()).nl()
			.append("Usage: ").append(trigger).append(name()).append(" [ \"!list\" | \"!random\" | item_number | search_term ]").nl()
			.append("Examples:").nl()
			.append(trigger).append(name()).append(" !list (lists all items)").nl()
			.append(trigger).append(name()).append(" !random (displays a random item)").nl()
			.append(trigger).append(name()).append(" 5 (displays item #5)").nl()
			.append(trigger).append(name()).append(" string (searches for items containing the word \"string\")")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent();

		/**
		 * Display the help text.
		 */
		if (content.isEmpty()) {
			return reply(helpText(context.getTrigger()), chatCommand);
		}

		/**
		 * Display all the items.
		 */
		if ("!list".equalsIgnoreCase(content)) {
			return displayItems(chatCommand, items);
		}

		/**
		 * Display a random item.
		 */
		if ("!random".equalsIgnoreCase(content)) {
			int itemNumber = ThreadLocalRandom.current().nextInt(items.size());
			Item item = items.get(itemNumber);
			return displayItem(chatCommand, item);
		}

		/**
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

		/**
		 * Search by keyword.
		 */
		content = content.toLowerCase();
		List<Item> searchResults = new ArrayList<>();
		for (Item item : items) {
			//@formatter:off
			boolean matchFound =
				item.title.toLowerCase().contains(content) ||
				item.summary.toLowerCase().contains(content);
			//@formatter:on

			if (matchFound) {
				searchResults.add(item);
			}
		}

		/**
		 * No search results found.
		 */
		if (searchResults.isEmpty()) {
			return reply("No matches found.", chatCommand);
		}

		/**
		 * One item found.
		 */
		if (searchResults.size() == 1) {
			return displayItem(chatCommand, searchResults.get(0));
		}

		/**
		 * Multiple items found.
		 */
		return displayItems(chatCommand, searchResults);
	}

	private ChatResponse displayItem(ChatCommand chatCommand, Item item) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(chatCommand);
		cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title));
		cb.nl().append(removeMarkdown(item.summary));
		cb.nl().append("(source: Effective Java, Third Edition by Joshua Bloch, p.").append(item.page).append(")");

		return new ChatResponse(cb, SplitStrategy.WORD);
	}

	private ChatResponse displayItems(ChatCommand chatCommand, List<Item> items) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(chatCommand);
		for (Item item : items) {
			cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title)).nl();
		}
		return new ChatResponse(cb, SplitStrategy.NEWLINE);
	}

	private static String removeMarkdown(String s) {
		return s.replaceAll("[`*]", "");
	}

	private static class Item {
		private final int number, page;
		private final String title, summary;

		private Item(Builder builder) {
			number = builder.number;
			page = builder.page;
			title = builder.title;
			summary = builder.summary;
		}

		public static class Builder {
			private int number, page;
			private String title, summary;

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

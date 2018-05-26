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
		String xmlFile = "effective-java.xml";
		Leaf document;
		try (InputStream in = getClass().getResourceAsStream(xmlFile)) {
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
			if (itemsByNumber.containsKey(number)) {
				throw new RuntimeException("Duplicate item number in " + xmlFile + ": " + number);
			}

			int page = Integer.parseInt(itemElement.attribute("page"));
			String title = itemElement.selectFirst("title").text();
			Leaf summaryElement = itemElement.selectFirst("summary");
			String summary = (summaryElement == null) ? null : summaryElement.text();

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
		for (int i = 1; true; i++) {
			Item item = itemsByNumber.get(i);
			if (item == null) {
				if (!itemsByNumber.isEmpty()) {
					throw new RuntimeException("Item number " + i + " is missing in " + xmlFile + ".");
				}
				break;
			}

			items.add(itemsByNumber.remove(i));
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
			.append("Usage: ").append(trigger).append(name()).append(" [ item_number | search_term ]").nl()
			.append("Examples:").nl()
			.append(trigger).append(name()).append(" (displays a random item)").nl()
			.append(trigger).append(name()).append(" 5 (displays item #5)").nl()
			.append(trigger).append(name()).append(" string (searches for items containing the word \"string\")")
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String content = chatCommand.getContent();

		/**
		 * Random item.
		 */
		if (content.isEmpty()) {
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
			//not an item number
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
				(item.summary != null && item.summary.toLowerCase().contains(content));
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
		ChatBuilder cb = new ChatBuilder();
		cb.reply(chatCommand);
		for (Item item : searchResults) {
			cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title)).nl();
		}
		return new ChatResponse(cb, SplitStrategy.NEWLINE);
	}

	private ChatResponse displayItem(ChatCommand chatCommand, Item item) {
		ChatBuilder cb = new ChatBuilder();
		cb.reply(chatCommand);
		cb.append("Item ").append(item.number).append(": ").append(removeMarkdown(item.title));
		String summary = (item.summary == null) ? "(summary not entered yet)" : item.summary;
		cb.nl().append(removeMarkdown(summary));
		cb.nl().append("(source: Effective Java, Third Edition by Joshua Bloch, p.").append(item.page).append(")");

		return new ChatResponse(cb, SplitStrategy.WORD);
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
				this.title = (title != null && title.isEmpty()) ? null : title;
				return this;
			}

			public Builder summary(String summary) {
				this.summary = (summary != null && summary.isEmpty()) ? null : summary;
				return this;
			}

			public Item build() {
				if (number <= 0) {
					throw new IllegalArgumentException("Valid item number required.");
				}
				if (page <= 0) {
					throw new IllegalArgumentException("Valid page number required.");
				}
				if (title == null) {
					throw new IllegalArgumentException("Title required.");
				}

				return new Item(this);
			}
		}
	}
}

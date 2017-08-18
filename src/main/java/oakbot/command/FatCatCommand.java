package oakbot.command;

import static oakbot.command.Command.reply;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import oakbot.Database;
import oakbot.bot.BotContext;
import oakbot.bot.ChatCommand;
import oakbot.bot.ChatResponse;
import oakbot.chat.ChatMessage;
import oakbot.util.ChatBuilder;

/**
 * Displays a random fat cat picture from a list of user-defined fat cat
 * pictures.
 * @author Michael Angstadt
 */
public class FatCatCommand implements Command {
	private final int hans = 4581014;
	private final Database db;
	private final List<String> cats = new ArrayList<>();
	private int isCatFatUserId;
	private String isCatFatUrl;

	public FatCatCommand(Database db) {
		this.db = db;

		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) db.get("fatcat");

		if (list != null) {
			cats.addAll(list);
		}
	}

	@Override
	public String name() {
		return "fatcat";
	}

	@Override
	public String description() {
		return "Displays a random fat cat.";
	}

	@Override
	public String helpText(String trigger) {
		//@formatter:off
		return new ChatBuilder()
			.append(trigger).append(name()).append(" : Show a random fat cat. ").nl()
			.append(trigger).append(name()).append(" list").append(" : List all fat cats. ").nl()
			.append(trigger).append(name()).append(" add URL").append(" : Add a fat cat. ").nl()
			.append(trigger).append(name()).append(" delete URL").append(" : Delete a fat cat. ").nl()
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String params[] = chatCommand.getContent().split("\\s+", 2);

		String action = params[0].toLowerCase();
		switch (action) {
		case "":
			return showCat(chatCommand);
		case "list":
			return listCats();
		case "add":
			return addCat(chatCommand, context, (params.length < 2) ? null : params[1]);
		case "delete":
			return deleteCat(chatCommand, context, (params.length < 2) ? null : params[1]);
		default:
			return reply("Unknown action.", chatCommand);
		}
	}

	private ChatResponse showCat(ChatCommand chatCommand) {
		if (cats.isEmpty()) {
			return reply("Error: No fat cats defined.", chatCommand);
		}

		String cat = Command.random(cats);
		return new ChatResponse(cat);
	}

	private ChatResponse listCats() {
		ChatBuilder cb = new ChatBuilder();

		if (cats.isEmpty()) {
			cb.italic("no fat cats defined");
		} else {
			for (String cat : cats) {
				cb.append("> ").append(cat).nl();
			}
		}

		return new ChatResponse(cb);
	}

	private ChatResponse addCat(ChatCommand chatCommand, BotContext context, String cat) {
		if (!context.isAuthorAdmin() && chatCommand.getMessage().getUserId() != hans) {
			return reply("Only Hans can add.", chatCommand);
		}

		if (cat == null) {
			//@formatter:off
			return reply(new ChatBuilder()
				.append("Specify the URL of the cat you want to add: ")
				.code()
				.append(context.getTrigger())
				.append(name())
				.append(" add URL")
				.code()
			.toString(), chatCommand);
			//@formatter:on
		}

		/*
		 * Just unescape the HTML entities. Do not convert to Markdown because
		 * this will escape special characters like underscores, which will
		 * break the URL.
		 */
		cat = StringEscapeUtils.unescapeHtml4(cat);

		if (cats.contains(cat)) {
			return reply("Cat already added.", chatCommand);
		}

		isCatFatUserId = chatCommand.getMessage().getUserId();
		isCatFatUrl = cat;
		return reply("Is cat fat? (y/n)", chatCommand);
	}

	private ChatResponse deleteCat(ChatCommand chatCommand, BotContext context, String cat) {
		if (!context.isAuthorAdmin() && chatCommand.getMessage().getUserId() != hans) {
			return reply("Only Hans can delete.", chatCommand);
		}

		if (cat == null) {
			//@formatter:off
			return reply(new ChatBuilder()
				.append("Specify the URL of the cat you want to delete: ")
				.code()
				.append(context.getTrigger())
				.append(name())
				.append(" delete URL")
				.code()
			.toString(), chatCommand);
			//@formatter:on
		}

		/*
		 * Just unescape the HTML entities. Do not convert to Markdown because
		 * this will escape special characters like underscores, which will
		 * break the URL.
		 */
		cat = StringEscapeUtils.unescapeHtml4(cat);

		boolean removed = cats.remove(cat);
		if (!removed) {
			return reply("404 cat not found.", chatCommand);
		}

		save();
		return reply("Deleted.", chatCommand);
	}

	public String handleResponse(ChatMessage message) {
		if (isCatFatUserId == 0) {
			return null;
		}

		if (message.getUserId() != isCatFatUserId) {
			return null;
		}

		String content = message.getContent();
		if (content == null || content.isEmpty()) {
			return null;
		}

		switch (Character.toLowerCase(content.charAt(0))) {
		case 'y':
			cats.add(isCatFatUrl);
			save();
			isCatFatUserId = 0;
			isCatFatUrl = null;
			return "Added.";
		case 'n':
			isCatFatUserId = 0;
			isCatFatUrl = null;
			return "Cat not added.  Cat must be fat.";
		default:
			return "Please answer yes or no.";
		}
	}

	private void save() {
		db.set("fatcat", cats);
	}
}

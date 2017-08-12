package oakbot.command;

import static oakbot.command.Command.reply;

import java.util.ArrayList;
import java.util.List;

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
			if (!context.isAuthorAdmin() && chatCommand.getMessage().getUserId() != hans) {
				return reply("Ask Hans.", chatCommand);
			}

			if (params.length < 2) {
				return reply("Specify the URL of the cat you want to add: `" + context.getTrigger() + name() + " add URL`", chatCommand);
			}

			String cat = ChatBuilder.toMarkdown(params[1], false);
			if (cats.contains(cat)) {
				return reply("Cat already added.", chatCommand);
			}

			isCatFatUserId = chatCommand.getMessage().getUserId();
			isCatFatUrl = cat;
			return reply("Is cat fat? (y/n)", chatCommand);
		case "delete":
			if (!context.isAuthorAdmin() && chatCommand.getMessage().getUserId() != hans) {
				return reply("Ask Hans.", chatCommand);
			}

			if (params.length < 2) {
				return reply("Specify the URL of the cat you want to delete: `" + context.getTrigger() + name() + " delete URL`", chatCommand);
			}

			cat = ChatBuilder.toMarkdown(params[1], false);
			return deleteCat(cat, chatCommand);
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

	private ChatResponse deleteCat(String cat, ChatCommand chatCommand) {
		boolean removed = cats.remove(cat);
		return reply(removed ? "Deleted." : "404 cat not found.", chatCommand);
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

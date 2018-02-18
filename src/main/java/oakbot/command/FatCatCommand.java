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
	private final Conversations conversations = new Conversations();

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
			.append(trigger).append(name()).append(" list : List all fat cats. ").nl()
			.append(trigger).append(name()).append(" add URL : Add a fat cat. ").nl()
			.append(trigger).append(name()).append(" delete URL : Delete a fat cat. ").nl()
		.toString();
		//@formatter:on
	}

	@Override
	public ChatResponse onMessage(ChatCommand chatCommand, BotContext context) {
		String params[] = chatCommand.getContent().split("\\s+", 2);

		String action = params[0].toLowerCase();
		String url = (params.length < 2) ? null : params[1];
		switch (action) {
		case "":
			return showCat(chatCommand);
		case "list":
			return listCats();
		case "add":
			return addCat(chatCommand, context, url);
		case "delete":
			return deleteCat(chatCommand, context, url);
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
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Specify the URL of the cat you want to add: ")
				.code()
				.append(context.getTrigger())
				.append(name())
				.append(" add URL")
				.code()
			);
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

		Conversation conversation = new Conversation(chatCommand.getMessage().getRoomId(), chatCommand.getMessage().getUserId(), cat);
		conversations.add(conversation);

		return reply("Is cat fat? (y/n)", chatCommand);
	}

	private ChatResponse deleteCat(ChatCommand chatCommand, BotContext context, String cat) {
		if (!context.isAuthorAdmin() && chatCommand.getMessage().getUserId() != hans) {
			return reply("Only Hans can delete.", chatCommand);
		}

		if (cat == null) {
			//@formatter:off
			return new ChatResponse(new ChatBuilder()
				.reply(chatCommand)
				.append("Specify the URL of the cat you want to delete: ")
				.code()
				.append(context.getTrigger())
				.append(name())
				.append(" delete URL")
				.code()
			);
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
		Conversation conversation = conversations.get(message.getRoomId(), message.getUserId());
		if (conversation == null) {
			return null;
		}

		String content = message.getContent().getContent();
		if (content.isEmpty()) {
			return null;
		}

		switch (Character.toLowerCase(content.charAt(0))) {
		case 'y':
			cats.add(conversation.url);
			save();
			conversations.remove(conversation);
			return "Added.";
		case 'n':
			conversations.remove(conversation);
			return "Cat not added.  Cat must be fat.";
		default:
			return "Please answer yes or no.";
		}
	}

	private void save() {
		db.set("fatcat", cats);
	}

	private class Conversations {
		private final List<Conversation> conversations = new ArrayList<>();

		public Conversation get(int roomId, int userId) {
			for (Conversation conversation : conversations) {
				if (conversation.roomId == roomId && conversation.userId == userId) {
					return conversation;
				}
			}
			return null;
		}

		public void add(Conversation conversation) {
			Conversation existing = get(conversation.roomId, conversation.userId);
			if (existing != null) {
				remove(existing);
			}

			conversations.add(conversation);
		}

		public void remove(Conversation conversation) {
			conversations.remove(conversation);
		}
	}

	private class Conversation {
		private final int roomId;
		private final int userId;
		private final String url;

		public Conversation(int roomId, int userId, String url) {
			this.roomId = roomId;
			this.userId = userId;
			this.url = url;
		}
	}
}

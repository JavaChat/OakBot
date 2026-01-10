package oakbot.command;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;
import static oakbot.bot.ChatActions.reply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.Database;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.listener.Listener;
import oakbot.util.ChatBuilder;
import oakbot.util.Rng;

/**
 * Displays a random fat cat picture from a list of user-defined fat cat
 * pictures.
 * @author Michael Angstadt
 */
public class FatCatCommand implements Command, Listener {
	private final Database db;
	private final List<Integer> commandAdmins;
	private final List<String> cats = new ArrayList<>();
	private final Conversations conversations = new Conversations();

	public FatCatCommand(Database db) {
		this(db, Collections.emptyList());
	}

	public FatCatCommand(Database db, List<Integer> commandAdmins) {
		this.db = db;
		this.commandAdmins = commandAdmins;

		@SuppressWarnings("unchecked")
		var list = (List<String>) db.get("fatcat");

		if (list != null) {
			cats.addAll(list);
		}
	}

	@Override
	public String name() {
		return "fatcat";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((Command)this)
			.summary("Displays a fat cat.")
			.example("", "Shows a random fat cat.")
			.example("list", "Lists all fat cats.")
			.example("add URL", "Adds a fat cat.")
			.example("delete URL", "Deletes a fat cat.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var params = chatCommand.getContent().split("\\s+", 2);

		var action = params[0].toLowerCase();
		var url = (params.length < 2) ? null : params[1];
		return switch (action) {
		case "" -> showCat(chatCommand);
		case "list" -> listCats();
		case "add" -> addCat(chatCommand, bot, url);
		case "delete" -> deleteCat(chatCommand, bot, url);
		default -> reply("Unknown action.", chatCommand);
		};
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		var reply = handleResponse(message);
		return (reply == null) ? doNothing() : reply(reply, message);
	}

	private ChatActions showCat(ChatCommand chatCommand) {
		if (cats.isEmpty()) {
			return reply("Error: No fat cats defined.", chatCommand);
		}

		var cat = Rng.random(cats);

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cat).bypassFilters(true)
		);
		//@formatter:on
	}

	private ChatActions listCats() {
		var cb = new ChatBuilder();

		if (cats.isEmpty()) {
			cb.italic("no fat cats defined");
		} else {
			for (var cat : cats) {
				cb.append("> ").append(cat).nl();
			}
		}

		//@formatter:off
		return ChatActions.create(
			new PostMessage(cb).splitStrategy(SplitStrategy.NEWLINE).bypassFilters(true)
		);
		//@formatter:on
	}

	private ChatActions addCat(ChatCommand chatCommand, IBot bot, String cat) {
		if (!hasEditPerms(chatCommand, bot)) {
			return reply("Only Hans can add.", chatCommand);
		}

		if (cat == null) {
			//@formatter:off
			return post(new ChatBuilder()
				.append("Specify the URL of the cat you want to add: ")
				.code()
				.append(bot.getTrigger())
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

		var conversation = new Conversation(chatCommand.getMessage().roomId(), chatCommand.getMessage().userId(), cat);
		conversations.add(conversation);

		return reply("Is cat fat? (y/n)", chatCommand);
	}

	private ChatActions deleteCat(ChatCommand chatCommand, IBot bot, String cat) {
		if (!hasEditPerms(chatCommand, bot)) {
			return reply("Only Hans can delete.", chatCommand);
		}

		if (cat == null) {
			//@formatter:off
			return reply(new ChatBuilder()
				.append("Specify the URL of the cat you want to delete: ")
				.code()
				.append(bot.getTrigger())
				.append(name())
				.append(" delete URL")
				.code(), chatCommand
			);
			//@formatter:on
		}

		/*
		 * Just unescape the HTML entities. Do not convert to Markdown because
		 * this will escape special characters like underscores, which will
		 * break the URL.
		 */
		cat = StringEscapeUtils.unescapeHtml4(cat);

		var removed = cats.remove(cat);
		if (!removed) {
			return reply("404 cat not found.", chatCommand);
		}

		save();
		return reply("Deleted.", chatCommand);
	}

	private boolean hasEditPerms(ChatCommand chatCommand, IBot bot) {
		var authorId = chatCommand.getMessage().userId();
		return bot.isAdminUser(authorId) || commandAdmins.contains(authorId);
	}

	private String handleResponse(ChatMessage message) {
		var conversation = conversations.get(message.roomId(), message.userId());
		if (conversation == null) {
			return null;
		}

		var content = message.content().getContent();
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

	private static class Conversations {
		private final List<Conversation> list = new ArrayList<>();

		public Conversation get(int roomId, int userId) {
			//@formatter:off
			return list.stream()
				.filter(conversation -> conversation.roomId == roomId)
				.filter(conversation -> conversation.userId == userId)
			.findFirst().orElse(null);
			//@formatter:on
		}

		public void add(Conversation conversation) {
			var existing = get(conversation.roomId, conversation.userId);
			if (existing != null) {
				remove(existing);
			}

			list.add(conversation);
		}

		public void remove(Conversation conversation) {
			list.remove(conversation);
		}
	}

	private record Conversation(int roomId, int userId, String url) {
	}
}

package oakbot.listener.chatgpt;

import static java.util.function.Predicate.not;
import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.ai.openai.OpenAIException;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Generates images based on the latest messages in the chat room using various
 * AI image models.
 * @author Michael Angstadt
 */
public class ImagineContextCommand implements Command {
	private final ImagineCore core;
	private final String defaultModel;
	private final int maxMessageLength;
	private final int contextSize;

	public ImagineContextCommand(ImagineCore core, String defaultModel, int maxMessageLength, int contextSize) {
		this.core = core;
		this.defaultModel = defaultModel;
		this.maxMessageLength = maxMessageLength;
		this.contextSize = contextSize;
	}

	@Override
	public String name() {
		return "imagine-context";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Creates an image based on the latest " + contextSize + " messages in the chat room using OpenAI and Stability.ai.")
			.detail(core.helpDetail())
			.example("", "Generates an image using " + defaultModel + ".")
			.example("gpt-image-2", "Include the model ID at the beginning of the message to define which model to use.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var quotaReached = core.checkQuota(chatCommand);
		if (quotaReached != null) {
			return quotaReached;
		}

		var model = chatCommand.getContent();
		if (model.isEmpty()) {
			model = defaultModel;
		}

		if (!ImagineCore.supportedModels.contains(model)) {
			return reply(new ChatBuilder().append("Model ").code(model).append(" not recognized."), chatCommand);
		}

		List<ChatMessage> messagesAscending;
		try {
			messagesAscending = bot.getLatestMessages(chatCommand.getMessage().roomId(), contextSize * 2);
		} catch (IOException e) {
			return error("Problem getting chat message transcript.", e, chatCommand);
		}

		var messagesDescending = new ArrayList<>(messagesAscending);
		Collections.reverse(messagesDescending);

		//@formatter:off
		var messagesFiltered = messagesDescending.stream()
			.filter(not(ChatMessage::isDeleted))
			.filter(not(ChatMessage::isUserSystemBot))
			.filter(message -> !isMessageInvokingThisCommand(message, bot))
			.limit(contextSize)
		.collect(Collectors.toCollection(ArrayList::new)); //mutable
		//@formatter:on

		if (messagesFiltered.isEmpty()) {
			return reply("No useable chat messages found.", chatCommand);
		}

		Collections.reverse(messagesFiltered);

		//@formatter:off
		var transcript = messagesFiltered.stream()
			.map(message -> {
				var content = message.content().getContent();
				var contentToUse = (maxMessageLength > 0 && content.length() > maxMessageLength) ? content.substring(0, maxMessageLength) : content;
				return message.username() + ": " + contentToUse;
			})
		.collect(Collectors.joining("\n"));
		//@formatter:off

		var prompt = "Imagine what this scene would look like if it were in a movie. Anime style. Each actor wears a name tag.\n\n" + transcript;

		try {
			var messagesToPost = core.generateImage(model, null, prompt.toString(), false, bot);

			core.logQuota(chatCommand, bot);

			var actions = new ChatActions();

			//@formatter:off
			messagesToPost.stream()
				.map(message -> new PostMessage(message).bypassFilters(true).splitStrategy(SplitStrategy.WORD))
			.forEach(actions::addAction);
			//@formatter:on

			return actions;
		} catch (OpenAIException | IOException | URISyntaxException e) {
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code(), chatCommand);
		}
	}

	private boolean isMessageInvokingThisCommand(ChatMessage message, IBot bot) {
		return message.content().getContent().startsWith(bot.getTrigger() + name());
	}
}
